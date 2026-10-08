# 🐶 스마트 로봇견 제어 통합 시스템 (DOG_04)

ESP32와 각종 센서를 활용하여 서버 및 젯슨 나노와 통신하는 스마트 로봇견 제어 시스템입니다. 

## 🛠 주요 기능 (Features)
1. **커스텀 픽셀 표정 출력**: I2C 1602 LCD를 활용해 상태에 따른 표정(웃음, 슬픔, 모름) 픽셀 아트 출력
2. **터치 인식**: FSR 압력 센서를 이용해 머리 쓰다듬기, 등 터치 등 사용자의 상호작용 인식
3. **오디오 재생**: SD 카드 버퍼링을 통한 MP3 재생 (짖기, 앓는 소리 등 돌발 미션 수행)
4. **젯슨 나노 연동**: 비전 및 음성 인식 실패 시그널(`UNKNOWN_CMD`) 수신 및 상태 반영

## 📡 서버 통신 및 폴링(Polling) 최적화
본 프로젝트는 서버 부하를 줄이고 안정성을 높이기 위해 아래와 같은 통신 아키텍처를 가집니다.

* **동적 명령어 폴링 (Dynamic Polling)**
  * 기본적으로 30초 주기(`commandPollInterval`)로 서버에 명령을 요청합니다.
  * 서버의 응답 JSON에 포함된 `nextPollIntervalSec` 값을 파싱하여 다음 폴링 주기를 유동적으로 조절함으로써 불필요한 트래픽을 방지합니다.
* **오프라인 데이터 버퍼링 (Offline Buffer)**
  * Wi-Fi 또는 서버 연결이 끊겼을 때, 센서 상태 데이터를 SD 카드의 `/buffer.txt`에 임시 저장합니다.
  * 통신이 복구되면 모아둔 데이터를 서버로 일괄 전송(Burst 송신)하여 데이터 유실을 방지합니다.

## 💻 사용된 라이브러리 (Dependencies)
* `WiFi.h`, `HTTPClient.h` : REST API 통신
* `ArduinoJson` : JSON 데이터 파싱 및 생성
* `ESP32-audioI2S` (`Audio.h`) : I2S 오디오 출력
* `LiquidCrystal_I2C` : 1602 LCD 제어
* `SD_MMC.h` : 오디오 파일 및 오프라인 버퍼용 SD 카드 제어

## 🚀 설치 및 실행 방법 (How to Use)
본 저장소에는 보안을 위해 민감한 Wi-Fi 및 서버 정보가 제외되어 있습니다. 코드를 실행하려면 아래 단계를 따라주세요.

1. 본 저장소를 다운로드(Clone) 합니다.
2. `secrets_example.h` 파일의 이름을 `secrets.h` 로 변경합니다.
3. `secrets.h` 파일을 열고 본인의 환경에 맞게 정보를 수정합니다.
   ```cpp
   // secrets.h 예시
   const char* ssid = "본인의_와이파이_이름";
   const char* password = "와이파이_비밀번호";
   const char* BASE_URL = "http://서버_IP주소:포트";
