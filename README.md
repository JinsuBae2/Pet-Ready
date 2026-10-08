# Pet-Ready

로봇강아지를 돌보면서 반려견 입양 전에 양육을 미리 경험해 보는 프로젝트다.
영남이공대학교 소프트웨어융합과 캡스톤 디자인으로 4명이 함께 만들었다. (2026.04 – 06)

## 왜 만들었나

교수님께서 미국에서는 아이들 교육에 IoT를 활용한다는 이야기를 해 주신 게 시작이었다.
반려동물을 쉽게 입양했다가 파양하는 일이 많은데, 입양 전에 밥 주기, 산책, 훈련 같은 일을 직접 겪어 보면 줄일 수 있지 않을까 생각했다.
아이들이 생명에 대한 책임감을 배우는 교육용으로도 쓸 수 있겠다고 봤다.

## 어떻게 동작하나

- 로봇강아지(ESP32)는 쓰다듬기 같은 터치를 감지하고, LCD 표정과 소리로 반응한다.
- 사용자는 Android 앱에서 미션을 받고, 산책을 기록하고, 점수를 확인한다.
- Jetson Nano 카메라는 밥그릇(YOLO)과 손동작(MediaPipe)을 인식해서 서버로 보낸다.
- Spring Boot 백엔드가 이 신호들을 모아 미션과 점수를 관리하고, 앱으로 FCM 알림을 보낸다.
- 시뮬레이션이 끝나면 점수를 바탕으로 Gemini API가 사람마다 다른 조언이 담긴 최종 리포트를 만든다.
  모두 같은 템플릿을 받는 것보다 각자에게 맞는 조언을 주고 싶어서 넣었다.

## 누가 무엇을 했나

| 파트 | 담당 |
| --- | --- |
| Spring Boot 백엔드 | 배진수 ([@JinsuBae2](https://github.com/JinsuBae2)) |
| Jetson Nano 비전 | 배진수 |
| 로봇강아지 (Arduino / ESP32) | 팀원 2명 |
| Android 앱 | 팀원 1명 |

## 만들면서 겪은 것

4명이 각자 다른 파트를 맡았고, 다들 협업이 처음이었다.
API 형식은 문서로 정했지만 막상 붙여 보면 서로 안 맞는 코드가 많았고, 통신이 안 되는 경우도 많았다.

기억에 남는 건 로봇강아지 ID 문제다.
로봇강아지는 한 대뿐인데 아두이노 코드는 `DOG_04`를, 비전과 백엔드는 `DOG_01`을 쓰고 있어서 연동이 되지 않았다.
결국 `DOG_01`로 맞췄다. 처음 버전은 `arduino/DOG_04_System/`에, 맞춘 버전은 `arduino/DOG_01_Project/`에 남아 있다.

백엔드와 비전 코드는 AI 개발 도구를 쓰면서 작성했다.
첫 프로젝트라 AI를 제대로 활용하지 못했고, 프롬프트도 구체적이지 않았다.

발표는 현장 시연 대신 영상으로 했다.

## 폴더 구조

```text
pet-ready-backend/      Spring Boot 백엔드 (Java 17, MariaDB)
pet-ready-android/      Android 앱 (Java)
arduino/
  DOG_01_Project/       로봇강아지 최종 코드 (DOG_01)
  DOG_04_System/        로봇강아지 이전 버전 (DOG_04)
vision/
  vision_bowl_local_detector_event.py   Jetson Nano 비전 스크립트
  legacy/               비전 스크립트 이전 버전
docs/
  안드로이드_로직_설명서.md   Android 앱 화면 흐름과 로직 설명
```

처음에는 아두이노 코드와 비전 스크립트가 루트에 흩어져 있었는데, 프로젝트가 끝난 뒤 파트별 폴더로 정리했다.

## 실행 방법

### 백엔드

`pet-ready-backend/.env`에 DB 계정과 API 키를 넣는다.

```env
MARIADB_USER=DB_사용자
MARIADB_PASSWORD=DB_비밀번호
PUBLIC_DATA_API_KEY=공공데이터포털_유기동물_API_키
GEMINI_API_KEY=Gemini_API_키
```

`run_server.sh`가 `.env`를 읽어서 서버를 실행한다.

```bash
cd pet-ready-backend
nohup ./run_server.sh > backend.log 2>&1 &
tail -f backend.log
```

API 문서는 서버 실행 후 `/swagger-ui.html`에서 볼 수 있다.

### Jetson Nano 비전

YOLO 모델 파일(`yolov8n.pt`)은 저장소에 없으니 `vision/` 폴더에 넣는다. 스크립트는 실행한 위치에서 모델을 찾는다.

```bash
sudo chmod 666 /dev/video0
cd vision
sudo ~/pet_venv/bin/python3 vision_bowl_local_detector_event.py
```

### 로봇강아지 (ESP32)

Arduino IDE로 `arduino/DOG_01_Project/DOG_01_Project.ino`를 연다. 같은 폴더의 `secrets_example.h`를 복사해서 `secrets.h`를 만들고, Wi-Fi 정보와 백엔드 주소(`BASE_URL`)를 넣은 뒤 업로드한다.

### Android

`app/google-services.example.json`을 참고해서 본인 Firebase 프로젝트의 `google-services.json`을 `pet-ready-android/app/`에 넣는다.

## 주요 API

| 보내는 쪽 | API |
| --- | --- |
| 로봇강아지 | `POST /api/v1/pet/status`, `POST /api/v1/device/bark-event`, `GET /api/v1/pet/command/{deviceId}`, `POST /api/v1/pet/command/ack/{commandId}` |
| Jetson Nano | `POST /api/v1/device/vision-event`, `POST /api/v1/jetson/vision-sync`, `POST /api/v1/training/gesture` |
| Android 앱 | `POST /api/v1/auth/login`, `GET /api/v1/mission/today`, `POST /api/v1/walk/end`, `GET /api/v1/report/final` 등 |

전체 목록은 Swagger에서 확인할 수 있다.

## 브랜치

코드는 `develop` 브랜치에 있다. `main`에는 초기 README만 있다.
