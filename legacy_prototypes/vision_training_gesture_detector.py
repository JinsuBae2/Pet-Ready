import cv2
import requests
import time
import sys

# MediaPipe 패키지가 설치되어 있지 않은 경우를 대비한 동적 임포트 및 예외 처리
try:
    import mediapipe as mp
except ImportError:
    print("❌ 'mediapipe' 라이브러리가 설치되어 있지 않습니다.")
    print("💡 연동 테스트를 위해서는 터미널에서 다음 명령을 실행하여 설치해 주세요: pip install mediapipe")
    # 스크립트가 튕기지 않고 안내 후 종료되도록 방어 처리
    mp = None

# 1. 로컬 환경 설정 변수
SERVER_URL = 'http://localhost:8080'  # 백엔드 서버 베이스 주소
DEVICE_ID = 'DOG_01'
REQUIRED_FRAMES = 15  # 디바운싱 판정을 위한 연속 감지 프레임 수
COOLDOWN_SEC = 5.0    # API 중복 전송 방지를 위한 쿨다운 시간 (5초)

def initialize_camera():
    backends = [cv2.CAP_AVFOUNDATION, cv2.CAP_ANY]
    for index in range(5):
        for backend in backends:
            try:
                cap = cv2.VideoCapture(index, backend)
                if cap.isOpened():
                    ret, test_frame = cap.read()
                    if ret and test_frame is not None:
                        import numpy as np
                        mean_brightness = np.mean(test_frame)
                        if mean_brightness > 2.0:
                            print(f"✅ 카메라 장치 인덱스 [{index}] 연결 성공!")
                            return cap
                    cap.release()
            except Exception:
                continue
    print("⚠️ 실제 카메라를 탐색하지 못했습니다. 기본 장치(0번) 연결을 시도합니다.")
    return cv2.VideoCapture(0)

def send_gesture_event(gesture_type, confidence=1.0):
    url = f"{SERVER_URL}/api/v1/training/gesture"
    payload = {
        "deviceId": DEVICE_ID,
        "gestureType": gesture_type,
        "confidence": confidence
    }
    try:
        res = requests.post(url, json=payload, timeout=3)
        if res.status_code == 200:
            print(f"📡 [서버 전송 성공] 제스쳐: {gesture_type} (신뢰도: {confidence*100:.1f}%)")
            return True
        else:
            print(f"⚠️ 백엔드 응답 에러 (Status: {res.status_code})")
    except Exception as e:
        print(f"❌ 로컬 서버 통신 실패 (스프링부트 가동 상태 확인 필수): {e}")
    return False

def main():
    if mp is None:
        print("MediaPipe가 누락되어 비전 감지 루프를 실행할 수 없습니다. 스크립트를 종료합니다.")
        sys.exit(1)

    print("🐾 [Pet-Ready Gesture Vision AI] 실시간 훈련 제스처 감지기 가동...")
    print("🔍 탐지 제스처 규격:")
    print("   - SIT(앉아)  ➡️ 검지손가락 하나만 똑바로 세우는 모양 (가리키기)")
    print("   - STAY(대기) ➡️ 다섯 손가락을 모두 쫙 펴는 모양 (정지/멈춤)")
    print(f"📡 백엔드 서버 ({SERVER_URL}) 연동 가동 중...")

    # MediaPipe Hands 모듈 초기화
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=1,
        min_detection_confidence=0.7,
        min_tracking_confidence=0.7
    )
    mp_draw = mp.solutions.drawing_utils

    cap = initialize_camera()
    if cap is None or not cap.isOpened():
        print("❌ 카메라 장치를 열 수 없습니다. 카메라가 정상 연결되었는지 확인해 주세요.")
        return

    # 디바운싱 및 통제용 상태 변수
    current_detect_type = "NONE"
    detect_frame_counter = 0
    last_sent_time = 0
    sent_lock_gesture = "NONE"

    cv2.namedWindow('Pet-Ready Training Gesture Detector', cv2.WINDOW_NORMAL)

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            print("❌ 프레임을 읽을 수 없습니다.")
            break

        # 좌우 반전 처리 (거울 모드) 및 RGB 변환
        frame = cv2.flip(frame, 1)
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # 랜드마크 분석 추론
        results = hands.process(rgb_frame)
        
        detected_gesture = "NONE"
        hand_confidence = 0.0

        if results.multi_hand_landmarks and results.multi_handedness:
            for hand_landmarks, handedness in zip(results.multi_hand_landmarks, results.multi_handedness):
                hand_confidence = handedness.classification[0].score
                
                # 랜드마크 그리기
                mp_draw.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)
                
                # 손가락 펴짐 여부 판정 (Y좌표 비교: 팁 Y < PIP 마디 Y 일 때 펴진 것)
                landmarks = hand_landmarks.landmark
                
                # 엄지손가락은 X좌표 거리 및 Y좌표 하이브리드 판정
                is_thumb_open = abs(landmarks[4].x - landmarks[2].x) > 0.08
                is_index_open = landmarks[8].y < landmarks[6].y
                is_middle_open = landmarks[12].y < landmarks[10].y
                is_ring_open = landmarks[16].y < landmarks[14].y
                is_pinky_open = landmarks[20].y < landmarks[18].y

                # 1. SIT 판정: 오직 검지만 펼쳐져 있고 중지, 약지, 새끼는 명확히 접힌 상태
                if is_index_open and not is_middle_open and not is_ring_open and not is_pinky_open:
                    detected_gesture = "SIT"
                # 2. STAY 판정: 검지, 중지, 약지, 새끼가 모두 펼쳐진 상태
                elif is_index_open and is_middle_open and is_ring_open and is_pinky_open:
                    detected_gesture = "STAY"

        # 쿨다운 락 해제 체크
        current_time = time.time()
        if current_time - last_sent_time > COOLDOWN_SEC:
            sent_lock_gesture = "NONE"

        # 디바운싱 로직 (연속된 프레임 수 누적)
        if detected_gesture != "NONE" and detected_gesture != sent_lock_gesture:
            if current_detect_type == detected_gesture:
                detect_frame_counter += 1
            else:
                current_detect_type = detected_gesture
                detect_frame_counter = 1
                
            # 디바운싱 완료 판정 및 전송
            if detect_frame_counter >= REQUIRED_FRAMES:
                print(f"🎯 [{detected_gesture} 제스쳐 확정] 프레임 조건 충족 ➡️ 백엔드로 전송 시도")
                if send_gesture_event(detected_gesture, hand_confidence):
                    last_sent_time = current_time
                    sent_lock_gesture = detected_gesture
                detect_frame_counter = 0
                current_detect_type = "NONE"
        else:
            detect_frame_counter = 0
            current_detect_type = "NONE"

        # 화면 UI 렌더링
        h, w, _ = frame.shape
        
        # 쿨다운 락 상태 노출
        if sent_lock_gesture != "NONE":
            remaining = int(COOLDOWN_SEC - (current_time - last_sent_time))
            cv2.rectangle(frame, (0, 0), (w, h), (0, 255, 0), 6)
            cv2.putText(frame, f"LOCKOUT ACTIVE: SENT {sent_lock_gesture} ({remaining}s)", (30, h - 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        else:
            cv2.rectangle(frame, (0, 0), (w, h), (200, 200, 200), 2)

        # 감지 상태 게이지 출력
        if current_detect_type != "NONE" and sent_lock_gesture == "NONE":
            progress_w = int((w - 60) * (detect_frame_counter / REQUIRED_FRAMES))
            cv2.line(frame, (30, 40), (30 + progress_w, 40), (0, 165, 255), 8)
            cv2.rectangle(frame, (30, 35), (w - 30, 45), (150, 150, 150), 1)
            cv2.putText(frame, f"Detecting... {current_detect_type} ({detect_frame_counter}/{REQUIRED_FRAMES})", (30, 75),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 165, 255), 2)
        else:
            cv2.putText(frame, "STATUS: SEARCHING GESTURE", (30, 50),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)

        cv2.imshow('Pet-Ready Training Gesture Detector', frame)

        # 'q' 키 누르면 수동 종료
        if cv2.waitKey(1) & 0xFF == ord('q'):
            print("🛑 사용자에 의한 수동 종료.")
            break

    cap.release()
    cv2.destroyAllWindows()
    hands.close()

if __name__ == "__main__":
    main()
