/*
  ===================================================================
  🐶 스마트 로봇견 제어 통합 시스템 (DOG_01) - v2.3 연동 패치 완료본
  ===================================================================
  주요 기능:
  1. I2C 1602 LCD를 활용한 서버 연동 실시간 상태 텍스트(아스키) 출력
  2. FSR 센서를 이용한 터치 인식 (머리 쓰다듬기 등)
  3. SD 카드 기반 MP3 오디오 재생 (짖기, 앓는 소리)
  4. Wi-Fi 기반 서버 통신 (상태 보고, 오프라인 버퍼, 명령어 폴링)
  5. 젯슨 나노 연동 (비전/음성 인식 및 훈련 보상 피드백 반영)
  6. 평상시 초록불(Green) 점등, 경고/돌발상황 시 빨간불(Red) 점등
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
#include "secrets.h"                          // 보안 처리된 와이파이 정보
const char* DEVICE_ID = "DOG_01";             // 비전 AI 연동을 위해 DOG_01로 수정됨

Audio audio;                                  // 오디오 객체 생성
LiquidCrystal_I2C lcd(0x27, 16, 2);           // LCD 객체 생성

// ==========================================
// 📌 3. 타이머 및 상태 관리 전역 변수
// ==========================================
unsigned long lastStatusTime = 0;       
unsigned long lastCommandTime = 0;      
unsigned long commandPollInterval = 30000; 

unsigned long lastBarkTime = 0;         
unsigned long nextBarkInterval = 45000; 

bool isBarkingActive = false;           
String serverLedColor = "GREEN";        // 초기 상태 초록색
bool isUnknownState = false;            
unsigned long unknownStateStartTime = 0;

// LCD 텍스트 및 커맨드 전역 변수 (서버에서 받아옴)
String lcdText1 = "  System Ready  ";
String lcdText2 = "  Connecting..  ";
String lcdCmd = "";

// 화면 깜빡임 방지용 이전 텍스트 기억 변수
String lastDisplayedText1 = "";
String lastDisplayedText2 = "";

const int THRESHOLD = 2600;             // 터치 임계값

// ==========================================
// 📌 4. 하드웨어 제어 및 표출 유틸리티 함수
// ==========================================

// 💡 LED 켜고 끄기 헬퍼 함수
void setLED(bool red, bool green) {
  digitalWrite(LED_R, red ? HIGH : LOW);
  digitalWrite(LED_G, green ? HIGH : LOW);
}

// 💡 LCD 및 LED 상태 업데이트 (평상시 초록불 적용)
void updateDisplayAndLED() {
  
  // 1. LED 업데이트 (빨간불 vs 초록불 처리)
  // 젯슨나노 인식 실패(isUnknownState) 로직 처리용 (2초 후 해제)
  if (isUnknownState) {
    if (millis() - unknownStateStartTime > 2000) {
      isUnknownState = false; 
    }
  }

  // 서버 지시가 RED 이거나, 돌발 짖기 중이거나, 인식 실패 상태일 때만 빨간불
  if (serverLedColor == "RED" || isBarkingActive || isUnknownState) {
    setLED(true, false);  // 🔴 빨간불 ON, 초록불 OFF
  } else {
    setLED(false, true);  // 🟢 평상시: 빨간불 OFF, 초록불 ON
  }

  // 2. LCD 실시간 텍스트 2줄 렌더링 (화면 깜빡임 방지 적용)
  if (lcdText1 != lastDisplayedText1 || lcdText2 != lastDisplayedText2) {
    lcd.clear();
    
    // Line 1 출력
    lcd.setCursor(0, 0);
    lcd.print(lcdText1);
    
    // Line 2 출력
    lcd.setCursor(0, 1);
    lcd.print(lcdText2);
    
    // 현재 표시된 텍스트 저장
    lastDisplayedText1 = lcdText1;
    lastDisplayedText2 = lcdText2;
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
// 📌 5. 오프라인 버퍼 기능 (네트워크 끊김 대비)
// ==========================================
void saveToBuffer(String data) {
  File file = SD_MMC.open("/buffer.txt", FILE_APPEND);
  if (file) {
    file.println(data);
    file.close();
  }
}

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

  if (success) SD_MMC.remove("/buffer.txt"); 
  http.end();
}

// ==========================================
// 📌 6. 서버 통신 API 함수 모음
// ==========================================

// 💡 현재 센서 상태 전송 및 서버 응답 파싱
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
    
    // 서버 가상 상태 색상 파싱
    if (responseDoc.containsKey("ledColor")) {
      serverLedColor = responseDoc["ledColor"].as<String>();
    }
    
    // LCD 텍스트 및 커맨드 파싱
    if (responseDoc.containsKey("lcdTextLine1")) {
      lcdText1 = responseDoc["lcdTextLine1"].as<String>();
    }
    if (responseDoc.containsKey("lcdTextLine2")) {
      lcdText2 = responseDoc["lcdTextLine2"].as<String>();
    }
    if (responseDoc.containsKey("lcdCommand")) {
      lcdCmd = responseDoc["lcdCommand"].as<String>();
    }
  } else {
    saveToBuffer(jsonBuffer); 
  }
  http.end();
}

void sendCommandAck(int commandId) {
  HTTPClient http;
  http.begin(String(BASE_URL) + "/api/v1/pet/command/ack/" + String(commandId));
  http.setTimeout(3000); 
  http.POST(""); 
  http.end();
}

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
// 📌 7. 돌발 상황(짖기 미션) 발생 로직
// ==========================================
void triggerSpontaneousBark() {
  isBarkingActive = true;
  audio.connecttoFS(SD_MMC, "/mung.mp3"); 
  sendBarkEventNotification();            
}

// ==========================================
// 📌 8. 명령어 폴링 (서버에서 할 일 가져오기)
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
      int nextPollIntervalSec = doc["nextPollIntervalSec"] | 30; 
      
      // 명령어 처리 분기점
      if (cmd == "SOUND_STOP") {
        audio.stopSong();
        isBarkingActive = false; 
      } 
      else if (cmd == "WHINE_START") {
        audio.connecttoFS(SD_MMC, "/GGing.mp3"); 
      } 
      else if (cmd == "UNKNOWN_CMD") {
        isUnknownState = true;           
        unknownStateStartTime = millis(); 
      }
      
      sendCommandAck(commandId); 
      commandPollInterval = nextPollIntervalSec * 1000;
    } else {
      commandPollInterval = 30000; 
    }
  }
  http.end();
}

// ==========================================
// 📌 9. 초기화 (Setup)
// ==========================================
void setup() {
  Serial.begin(115200);
  delay(2000); 

  // LCD 초기화
  Wire.begin(8, 9); 
  lcd.init();
  lcd.backlight();
  lcd.clear();

  // 핀 모드 설정
  pinMode(FSR_HEAD, INPUT);
  pinMode(FSR_BACK1, INPUT);
  pinMode(FSR_BACK2, INPUT);
  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);

  // 초기 상태 표시 (초록불 켜짐)
  updateDisplayAndLED();

  // SD 카드 연동
  SD_MMC.setPins(SD_MMC_CLK, SD_MMC_CMD, SD_MMC_D0);
  if (SD_MMC.begin("/sdcard", true)) {
    Serial.println("✅ SD 카드 마운트 성공!");
  }

  // 오디오 핀 설정
  audio.setPinout(13, 14, 15);
  audio.setVolume(15); 

  // Wi-Fi 연결
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { 
    delay(500); 
  }
  
  // 시간 동기화
  configTime(9 * 3600, 0, "pool.ntp.org", "time.nist.gov");
  
  // 짖기 주기 랜덤화 초기화
  randomSeed(analogRead(0) + millis());
  nextBarkInterval = random(30000, 90000); 

  // 시작 타이머 동기화
  unsigned long now = millis();
  lastStatusTime = now;
  lastCommandTime = now;
  lastBarkTime = now; 
}

// ==========================================
// 📌 10. 메인 루프 (Loop)
// ==========================================
void loop() {
  audio.loop(); 
  
  int headVal = analogRead(FSR_HEAD);
  int back1Val = analogRead(FSR_BACK1);
  int back2Val = analogRead(FSR_BACK2);
  
  if (WiFi.status() == WL_CONNECTED) {
    unsigned long currentMillis = millis();
    
    // ① 30초 주기로 상태 전송 API 호출
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

    // ③ 돌발 짖기 미션 발생 조건 체크
    if (!isBarkingActive && (currentMillis - lastBarkTime >= nextBarkInterval)) {
      triggerSpontaneousBark();
      lastBarkTime = currentMillis;
    }

    // ④ 돌발 짖기 진행 중 터치 인식 처리
    if (isBarkingActive) {
      if (!audio.isRunning()) {
        audio.connecttoFS(SD_MMC, "/mung.mp3");
      }

      // 머리 쓰다듬기(임계값 초과) 인식 시 중단
      if (headVal > THRESHOLD) {
        isBarkingActive = false; 
        audio.stopSong();        
        
        collectAndSendData(headVal, back1Val, back2Val); 
        lastStatusTime = millis(); 
        nextBarkInterval = random(60000, 180000); 
      }
    }

    // ⑤ 최신 상태에 맞춰 텍스트 및 LED 업데이트 실행
    updateDisplayAndLED();
  }
}
