# Pet-Ready
충동 입양 및 파양률 감소를 위한 IoT 연동 사전 양육 시뮬레이션


# 🐶 펫-레디 (Pet-Ready) - Backend
> **반려견 양육 시뮬레이터 (Puppy Parenting Simulator)**
> 
> 실제 반려견 양육의 고충(새벽 짖음, 배고픔, 산책 등)을 IoT 인형으로 체험하고, 사용자의 양육 적합도를 데이터로 산출하는 프로젝트입니다.

---

## 🌿 Git Branch Strategy
본 프로젝트는 **Develop 브랜치 전략**을 사용하여 코드의 안정성을 유지합니다.

- **`main`**: 최종 시연 및 배포용 브랜치 (Phase 6 최종 발표 시점)
- **`develop`**: 모든 기능이 통합되는 메인 개발 브랜치
- **`feat/`**: 기능 단위 개발 브랜치 (예: `feat/backend-auth`, `feat/android-gps`)
- **`fix/`**: 버그 수정 및 긴급 패치 브랜치

### 💡 Branch Rules
1. 모든 기능 개발은 `develop`에서 분기합니다.
2. 작업 완료 후 `develop` 브랜치로 **Pull Request(PR)**를 생성합니다.
3. 관리자(Backend)의 코드 리뷰 및 승인 후 병합을 진행합니다.

---

## 🛠 디렉터리 통합 및 작업 프로세스 (중요)
팀원들은 아래 순서에 따라 작업을 진행하고 코드를 합쳐야 합니다.

1.  **레포지토리 클론**: `git clone [Repository-URL]`
2.  **브랜치 생성**: 반드시 `develop` 브랜치에서 자신의 기능 브랜치를 만듭니다.
    * `git checkout develop` -> `git pull origin develop` -> `git checkout -b feat/파트/기능`
3.  **코드 작성**: 자신의 담당 디렉터리 내에서만 작업합니다.
    * **백엔드**: `pet-ready-backend/` 폴더 내부
    * **안드로이드**: `pet-ready-android/` 폴더 내부
    * **아두이노**: `pet-ready-arduino/` 폴더 내부
4.  **로컬 테스트**: 코드를 올리기 전 반드시 로컬에서 빌드 및 실행 여부를 확인합니다.
5.  **Push 및 Pull Request(PR)**:
    * 작업 완료 후 자신의 브랜치를 Push하고, GitHub에서 `develop` 브랜치 방향으로 PR을 생성합니다.
    * **관리자(백엔드 담당자)의 승인**이 있어야 `develop`에 최종 병합됩니다.

## 📝 Commit Message Convention
협업의 효율성을 위해 커밋 메시지 접두사를 통일합니다.
- `Feat`: 새로운 기능 추가
- `Fix`: 버그 수정
- `Docs`: 문서 수정 (README, Wiki 등)
- `Chore`: 환경 설정, 빌드 업무 등
- `Refactor`: 코드 리팩토링 (기능 변경 없음)

---

## 📂 프로젝트 통합 디렉터리 구조 (Monorepo)
모든 파트(Android, Backend, Arduino)의 코드는 이 레포지토리 내에서 독립된 폴더로 관리됩니다. 자신의 작업 영역 외의 파일은 관리자의 승인 없이 수정하지 않습니다.

```text
pet-ready/ (Root)
├── pet-ready-backend/    # [Spring Boot 3.3.x] 백엔드 서버 소스 [cite: 11, 39]
│   ├── src/main/java/    # Java 소스 코드 및 비즈니스 로직 [cite: 40]
│   └── build.gradle      # 백엔드 의존성 설정 [cite: 61]
├── pet-ready-android/    # [Kotlin] 안드로이드 앱 소스 [cite: 37, 38]
│   ├── app/src/main/     # UI 및 API 연동 코드 [cite: 59, 74]
│   └── build.gradle      # 앱 빌드 및 SDK 설정 [cite: 37]
├── pet-ready-arduino/    # [ESP32] 아두이노 펌웨어 소스 [cite: 11, 45]
│   ├── pet_ready_main/   # 센서 제어 및 통신 코드 (.ino) [cite: 45]
│   └── libraries/        # 사용되는 아두이노 라이브러리 목록 [cite: 47]
└── docs/                 # 프로젝트 기획서, API 명세서(Swagger), 설계 도면 [cite: 49, 77]
```



---

## 📝 4. 개발 규칙 및 컨벤션
* **한국어 주석 필수**: 모든 코드의 핵심 로직에는 상세한 한국어 주석을 작성합니다.
* **커밋 메시지**: `[Feat] 기능 설명`, `[Fix] 수정 내용`, `[Docs] 문서 수정` 등의 형식을 유지합니다.
* **통합 테스트**: Phase 3와 Phase 4 말에 전체 시스템 통합 테스트를 진행하며, 이때 모든 파트의 코드가 최신 `develop` 브랜치에 반영되어 있어야 합니다.

---

**문의 및 관리**: 백엔드 담당자 (Git Manager) | **최종 수정일**: 2026.03.31
