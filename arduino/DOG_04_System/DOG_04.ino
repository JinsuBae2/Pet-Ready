/*
  ===================================================================
  🐶 스마트 로봇견 제어 통합 시스템 (DOG_04)
  ===================================================================
  주요 기능:
  1. I2C 1602 LCD를 활용한 커스텀 픽셀 표정 출력 (웃음, 슬픔, 모름)
  2. FSR 센서를 이용한 터치 인식 (머리 쓰다듬기 등)
  3. SD 카드 기반 MP3 오디오 재생 (짖기, 앓는 소리)
  4. Wi-Fi 기반 서버 통신 (상태 보고, 오프라인 버퍼, 명령어 폴링)
  5. 젯슨 나노 연동 (비전/음성 인식 실패 시그널 대응)
  ===================================================================
*/

#include <WiFi.h>               // Wi-Fi 연결용
#include <HTTPClient.h>         // HTTP REST API 통신용
#include <ArduinoJson.h>        // JSON 파싱 및 생성용
#include <Audio.h>              // I2S 오디오 재생용
#include <Wire.h>               // I2C 통신용 (LCD)
#include <LiquidCrystal_I2C.h>  // 1602 LCD 제어용
#include "SD_MMC.h"             // SD 카드 리더기 제어용
#include "time.h"               // NTP 서버 시간 동기화용

// ==========================================
// 📌 1. 핀 설정 (하드웨어 연결)
// ==========================================
const int FSR_HEAD = 4;   // 머리 터치 센서
const int FSR_BACK1 = 2;  // 등 터치 센서 1
const int FSR_BACK2 = 6;  // 등 터치 센서 2
const int LED_R = 47;     // 상태 표시 LED (빨강)
const int LED_G = 48;     // 상태 표시 LED (초록)

// SD_MMC 통신 핀 (ESP32-S3 등에 맞춘 기본 핀 매핑)
#define SD_MMC_CMD 38
#define SD_MMC_CLK 39
#define SD_MMC_D0  40

// ==========================================
// 📌 2. 네트워크 및 서버 설정
// ==========================================
#include "secrets.h"                          
const char* DEVICE_ID = "DOG_04";


// ==========================================
// 📌 3. 타이머 및 상태 관리 변수
// ==========================================
unsigned long lastStatusTime = 0;       // 마지막 상태 보고 시간
unsigned long lastCommandTime = 0;      // 마지막 명령 폴링 시간
unsigned long commandPollInterval = 30000; // 명령 폴링 주기 (DB 응답에 따라 가변 적용)

unsigned long lastBarkTime = 0;         // 마지막으로 스스로 짖은 시간
unsigned long nextBarkInterval = 45000; // 다음 스스로 짖을 때까지의 대기 시간 (랜덤)

bool isBarkingActive = false;           // 현재 돌발 짖기(미션) 진행 중 여부
String serverLedColor = "GREEN";        // 서버가 지시한 가상 상태 색상
bool isUnknownState = false;            // 젯슨나노 '모름(음성인식 실패)' 상태 여부
unsigned long unknownStateStartTime = 0;// 모름 상태가 시작된 시간 (2초 유지용)
int lastDisplayedFace = -1;             // 화면 깜빡임 방지용 이전 표정 기억 변수

const int THRESHOLD = 2600;             // FSR 압력 센서가 '터치됨'으로 인식할 임계값

// ==========================================
// 📌 4. LCD 커스텀 표정 데이터 (각 2x2 구성)
// ==========================================
// 웃음 눈
byte eye_smile_LT[8] = {B00001, B00011, B00110, B01100, B11000, B10000, B00000, B00000};
byte eye_smile_RT[8] = {B10000, B11000, B01100, B00110, B00011, B00001, B00000, B00000};
byte eye_smile_LB[8] = {B00000, B00000, B00000, B00000, B00000, B00000, B00000, B00000};
byte eye_smile_RB[8] = {B00000, B00000, B00000, B00000, B00000, B00000, B00000, B00000};

// 슬픔 눈 (T)
byte eye_sad_LT[8] = {B00000, B11111, B11111, B00011, B00011, B00011, B00011, B00011};
byte eye_sad_RT[8] = {B00000, B11111, B11111, B11000, B11000, B11000, B11000, B11000};
byte eye_sad_LB[8] = {B00011, B00011, B00011, B00011, B00000, B00000, B00000, B00000};
byte eye_sad_RB[8] = {B11000, B11000, B11000, B11000, B00000, B00000, B00000, B00000};

// 모름 눈 (?)
byte eye_unk_LT[8] = {B00111, B01111, B11000, B11000, B00000, B00011, B00111, B00000};
byte eye_unk_RT[8] = {B11100, B11110, B00011, B00011, B00000, B11000, B11100, B00000};
byte eye_unk_LB[8] = {B00000, B00000, B00000, B00000, B00000, B00000, B00000, B00000};
byte eye_unk_RB[8] = {B00000, B00000, B00000, B00000, B00000, B00000, B00000, B00000};

// ==========================================
// 📌 5. 하드웨어 제어 유틸리티 함수
// ==========================================

// 💡 LED 켜고 끄기 헬퍼 함수
void setLED(bool red, bool green) {
  digitalWrite(LED_R, red ? HIGH : LOW);
  digitalWrite(LED_G, green ? HIGH : LOW);
}

// 💡 LCD에 2x2 눈과 입을 그려주는 헬퍼 함수
void drawFace(byte lt[], byte rt[], byte lb[], byte rb[], String mouth) {
  // 상태 변경 시 0~3번 커스텀 문자를 새로 덮어씁니다 (LCD 문자 개수 제한 우회)
  lcd.createChar(0, lt);
  lcd.createChar(1, rt);
  lcd.createChar(2, lb);
  lcd.createChar(3, rb);

  // 왼쪽 눈
  lcd.setCursor(3, 0); lcd.write(0); lcd.write(1);
  lcd.setCursor(3, 1); lcd.write(2); lcd.write(3);

  // 입
  lcd.setCursor(7, 1); lcd.print(mouth);

  // 오른쪽 눈
  lcd.setCursor(11, 0); lcd.write(0); lcd.write(1);
  lcd.setCursor(11, 1); lcd.write(2); lcd.write(3);
}

// 💡 표정 상태 분석 및 LCD & LED 원클릭 업데이트 함수
void updateDisplayAndLED() {
  int currentFace = 0; // 기본값 0 (평상시)

  // 1순위: 젯슨나노 명령 미인식 판별 및 2초 유지
  if (isUnknownState) {
    if (millis() - unknownStateStartTime < 2000) {
      currentFace = 2; // 모름 표정
    } else {
      isUnknownState = false; // 2초 경과 시 상태 해제
    }
  }
  
  // 2순위: 짖음 미션 수행 중이거나 서버 응답이 RED일 때
  if (!isUnknownState) { 
    if (isBarkingActive || serverLedColor == "RED") {
      currentFace = 1; // 우는/슬픈 표정
    } else {
      currentFace = 0; // 평상시 표정
    }
  }

  // 💡 상태가 바뀌었을 때만 화면을 새로 그려서 '깜빡임' 방지
  if (currentFace != lastDisplayedFace) {
    lcd.clear(); // 화면 초기화
    
    // 상태별 표정 출력
    if (currentFace == 0) { 
      setLED(false, false);  // 🟢 평소: LED 끄기
      drawFace(eye_smile_LT, eye_smile_RT, eye_smile_LB, eye_smile_RB, "--");
    } 
    else if (currentFace == 1) { 
      setLED(true, false);   // 🔴 슬픔/돌발상황: 빨간색 켜기
      drawFace(eye_sad_LT, eye_sad_RT, eye_sad_LB, eye_sad_RB, "o ");
    } 
    else if (currentFace == 2) { 
      setLED(true, false);   // 🔴 모름: 빨간색 켜기
      drawFace(eye_unk_LT, eye_unk_RT, eye_unk_LB, eye_unk_RB, "--");
    }

    lastDisplayedFace = currentFace; // 현재 표정 저장
  }
}

// 💡 13자리 밀리초 타임스탬프 계산 (서버 전송용)
unsigned long long getEpochMilliSeconds() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) return 0; 
  time_t now = time(nullptr);
  return ((unsigned long long)now * 1000) + (millis() % 1000);
}

// ==========================================
// 📌 6. 오프라인 버퍼 기능 (네트워크 끊김 대비)
// ==========================================
// 통신 실패 시 SD카드에 JSON 데이터를 임시 저장
void saveToBuffer(String data) {
  File file = SD_MMC.open("/buffer.txt", FILE_APPEND);
  if (file) {
    file.println(data);
    file.close();
  }
}

// 저장된 임시 데이터를 서버에 몰아서 전송
void sendBufferedData() {
  if (!SD_MMC.exists("/buffer.txt")) return;
  File file = SD_MMC.open("/buffer.txt", FILE_READ);
  if (!file) return;

  HTTPClient http;
  http.begin(String(BASE_URL) + "/api/v1/pet/status");
  http.setTimeout(3000); 
  http.addHeader("Content-Type", "application/json");

  bool success = true;
  while (file.available()) {
    String line = file.readStringUntil('\n');
    if (line.length() > 0) {
      if (http.POST(line) != 200) {
        success = false; 
        break;
      }
    }
  }
  file.close();

  if (success) SD_MMC.remove("/buffer.txt"); // 모두 전송 성공 시 파일 삭제
  http.end();
}

// ==========================================
// 📌 7. 서버 통신 API 함수 모음
// ==========================================

// 💡 현재 센서 상태를 서버에 전송하고, 서버의 피드백(LED 색상)을 받아오는 함수
void collectAndSendData(int h, int b1, int b2) {
  StaticJsonDocument<256> doc;
  doc["deviceId"] = DEVICE_ID;
  doc["headTouch"] = (h > THRESHOLD);
  doc["backTouch1"] = (b1 > THRESHOLD);
  doc["backTouch2"] = (b2 > THRESHOLD);
  
  String jsonBuffer;
  serializeJson(doc, jsonBuffer);
  
  HTTPClient http;
  http.begin(String(BASE_URL) + "/api/v1/pet/status");
  http.setTimeout(3000); 
  http.addHeader("Content-Type", "application/json");
  int httpCode = http.POST(jsonBuffer);
  
  if (httpCode == 200) {
    String responseBody = http.getString();
    StaticJsonDocument<512> responseDoc;
    deserializeJson(responseDoc, responseBody);
    
    // 서버가 기기의 가상 상태(예: 배고픔)를 판별해 LED 색상을 알려줌
    if (responseDoc.containsKey("ledColor")) {
      serverLedColor = responseDoc["ledColor"].as<String>();
    }
  } else {
    saveToBuffer(jsonBuffer); // 통신 실패 시 SD카드 버퍼에 저장
  }
  http.end();
}

// 💡 수신한 명령어를 잘 받았다고 서버에 알려주는 함수
void sendCommandAck(int commandId) {
  HTTPClient http;
  http.begin(String(BASE_URL) + "/api/v1/pet/command/ack/" + String(commandId));
  http.setTimeout(3000); 
  http.POST(""); 
  http.end();
}

// 💡 강아지가 스스로 짖기 시작했을 때 서버에 이벤트 기록을 남기는 함수
void sendBarkEventNotification() {
  StaticJsonDocument<256> doc;
  doc["deviceId"] = DEVICE_ID;            
  doc["timestamp"] = getEpochMilliSeconds(); 

  String jsonBuffer;
  serializeJson(doc, jsonBuffer);

  HTTPClient http;
  http.begin(String(BASE_URL) + "/api/v1/device/bark-event"); 
  http.setTimeout(3000);
  http.addHeader("Content-Type", "application/json");
  http.POST(jsonBuffer);
  http.end();
}

// ==========================================
// 📌 8. 돌발 상황(짖기 미션) 발생 로직
// ==========================================
void triggerSpontaneousBark() {
  isBarkingActive = true;
  audio.connecttoFS(SD_MMC, "/mung.mp3"); // SD카드의 짖는 소리 재생 시작
  sendBarkEventNotification();            // 즉시 서버에 보고
}

// ==========================================
// 📌 9. 명령어 폴링 (서버에서 할 일 가져오기)
// ==========================================
void checkCommand() {
  HTTPClient http;
  http.begin(String(BASE_URL) + "/api/v1/pet/command/" + DEVICE_ID);
  http.setTimeout(3000); 
  int httpCode = http.GET();
  
  if (httpCode == 200) {
    StaticJsonDocument<256> doc;
    deserializeJson(doc, http.getString());
    
    bool hasCommand = doc["hasCommand"] | false;
    
    if (hasCommand) {
      int commandId = doc["commandId"] | 0;
      String cmd = doc["command"] | "";
      int nextPollIntervalSec = doc["nextPollIntervalSec"] | 30; // 서버가 지정한 다음 폴링 대기 시간
      
      // 명령어 처리 분기점
      if (cmd == "SOUND_STOP") {
        audio.stopSong();
        isBarkingActive = false; // 앱에서 강제로 조용히 시키는 명령
      } 
      else if (cmd == "WHINE_START") {
        audio.connecttoFS(SD_MMC, "/GGing.mp3"); // 배고픔/슬픔 앓는 소리
      } 
      else if (cmd == "UNKNOWN_CMD") {
        isUnknownState = true;           // 젯슨 나노가 음성인식 실패했음을 알림
        unknownStateStartTime = millis(); 
      }
      
      sendCommandAck(commandId); // 처리 완료 응답
      commandPollInterval = nextPollIntervalSec * 1000;
    } else {
      commandPollInterval = 30000; // 명령이 없으면 30초 후 다시 확인
    }
  }
  http.end();
}

// ==========================================
// 📌 10. 초기화 (Setup)
// ==========================================
void setup() {
  Serial.begin(115200);
  delay(2000); 

  // LCD 및 I2C 통신 핀 지정 (GPIO 8, 9)
  Wire.begin(8, 9); 
  lcd.init();
  lcd.backlight();
  lcd.clear();

  // 시작할 때만 잠깐 문구 띄워주기 (상태 업데이트 시 사라짐)
  lcd.setCursor(0, 0); lcd.print("  System Ready  ");

  // 센서 및 LED 핀 모드 설정
  pinMode(FSR_HEAD, INPUT);
  pinMode(FSR_BACK1, INPUT);
  pinMode(FSR_BACK2, INPUT);
  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);

  // SD 카드 마운트
  SD_MMC.setPins(SD_MMC_CLK, SD_MMC_CMD, SD_MMC_D0);
  if (SD_MMC.begin("/sdcard", true)) {
    Serial.println("✅ SD 카드 마운트 성공!");
  }

  // 오디오 출력 핀 및 볼륨 설정
  audio.setPinout(13, 14, 15);
  audio.setVolume(15); 

  // Wi-Fi 연결
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { 
    delay(500); 
  }
  
  // 시간 동기화 (NTP)
  configTime(9 * 3600, 0, "pool.ntp.org", "time.nist.gov");
  
  // 돌발 짖기 주기 랜덤화 초기화
  randomSeed(analogRead(0) + millis());
  nextBarkInterval = random(30000, 90000); // 30초 ~ 90초 사이 랜덤

  // 초기 상태 세팅
  setLED(false, false); 
  unsigned long now = millis();
  lastStatusTime = now;
  lastCommandTime = now;
  lastBarkTime = now; 
}

// ==========================================
// 📌 11. 메인 루프 (Loop)
// ==========================================
void loop() {
  audio.loop(); // 백그라운드 오디오 버퍼 지속 처리 (절대 지연/block 금지)
  
  // 현재 터치 센서 값 읽기
  int headVal = analogRead(FSR_HEAD);
  int back1Val = analogRead(FSR_BACK1);
  int back2Val = analogRead(FSR_BACK2);
  
  if (WiFi.status() == WL_CONNECTED) {
    unsigned long currentMillis = millis();
    
    // ① 30초 주기로 상태 전송 API 호출 (버퍼 데이터가 있으면 먼저 전송)
    if (currentMillis - lastStatusTime >= 30000) {
      sendBufferedData(); 
      collectAndSendData(headVal, back1Val, back2Val);
      lastStatusTime = currentMillis;
    }
    
    // ② 지정된 주기로 명령어 폴링 API 호출
    if (currentMillis - lastCommandTime >= commandPollInterval) {
      checkCommand();
      lastCommandTime = currentMillis;
    }

    // ③ 돌발 짖기 미션 발생 조건 체크 (짖는 중이 아닐 때 지정된 시간 경과 시)
    if (!isBarkingActive && (currentMillis - lastBarkTime >= nextBarkInterval)) {
      triggerSpontaneousBark();
      lastBarkTime = currentMillis;
    }

    // ④ 돌발 짖기 진행 중 처리 로직 (터치 인식 및 반복 재생)
    if (isBarkingActive) {
      
      // 소리가 멈추면 무한 반복 재생
      if (!audio.isRunning()) {
        audio.connecttoFS(SD_MMC, "/mung.mp3");
      }

      // 머리를 쓰다듬으면(터치) 짖음 중단 및 즉시 클리어 데이터 전송
      if (headVal > THRESHOLD) {
        isBarkingActive = false; 
        audio.stopSong();        
        
        collectAndSendData(headVal, back1Val, back2Val); 
        lastStatusTime = millis(); // 30초 주기 타이머 동기화 리셋
        nextBarkInterval = random(60000, 180000); // 다음 미션은 1~3분 후 랜덤 발생
      }
    }

    // ⑤ 최신 상태에 맞춰 텍스트 표정 및 LED 업데이트 실행
    updateDisplayAndLED();
  }
}
