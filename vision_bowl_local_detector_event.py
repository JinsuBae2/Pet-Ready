import cv2
import requests
import time
import numpy as np
from ultralytics import YOLO
import sys
import warnings
import logging

# 콘솔 로그 및 경고 오버헤드 차단
warnings.filterwarnings("ignore")
logging.getLogger("ultralytics").setLevel(logging.ERROR)

# 1. 로컬 환경 설정 변수
MODEL_PATH = 'yolov8n.pt'
SERVER_URL = 'http://localhost:8080'  # 백엔드 서버 베이스 주소
DEVICE_ID = 'https://q.me-qr.com/s7oh7juf'
CONFIDENCE_THRES = 0.35  # 접시 감도 조절을 위해 0.35로 완화
REQUIRED_FRAMES_BOWL = 15  # 밥그릇 안착 판정 프레임 수
REQUIRED_FRAMES_GESTURE = 15  # 제스쳐 판정 프레임 수
GESTURE_COOLDOWN_SEC = 5.0    # 제스쳐 API 중복 전송 방지 쿨다운 (5초)

# MediaPipe 동적 임포트 및 예외 처리
try:
    import mediapipe as mp
    # 속성 접근성 사전 검증을 통해 AttributeError 방지
    if mp is not None:
        _ = mp.solutions.hands
except (ImportError, AttributeError) as e:
    print(f"❌ 'mediapipe' 라이브러리가 작동하지 않거나 정상 로드되지 않았습니다 ({e}). 제스쳐 감지가 스킵됩니다.")
    mp = None


# 2. 카메라 하드웨어 초기화 함수
def initialize_camera():
    import platform
    current_os = platform.system()
    
    if current_os == "Linux":
        backends = [cv2.CAP_V4L2, cv2.CAP_ANY]
    else:
        backends = [cv2.CAP_AVFOUNDATION, cv2.CAP_ANY]
        
    for index in range(3):
        for backend in backends:
            try:
                cap = cv2.VideoCapture(index, backend)
                if cap.isOpened():
                    # 리눅스 로지텍 C310 웹캠 호환성을 극대화하기 위해 포맷과 해상도 명시적 세팅
                    if current_os == "Linux":
                        cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*'MJPG'))
                        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1) # 버퍼 크기 1로 고정하여 밀림 렉 원천 차단
                    
                    ret, test_frame = cap.read()
                    if ret and test_frame is not None:
                        mean_brightness = np.mean(test_frame)
                        print(f"✅ 카메라 장치 인덱스 [{index}] (백엔드: {backend}, 밝기: {mean_brightness:.2f}) 연결 성공!")
                        return cap
                    cap.release()
            except Exception as e:
                print(f"⚠️ 인덱스 [{index}] 백엔드 [{backend}] 탐색 예외 발생: {e}")
                continue
    print("⚠️ 실제 카메라를 탐색하지 못했습니다. 기본 장치(0번) 연결을 재시도합니다.")
    cap = cv2.VideoCapture(0, cv2.CAP_V4L2 if current_os == "Linux" else cv2.CAP_ANY)
    if current_os == "Linux" and cap.isOpened():
        cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*'MJPG'))
        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    return cap

# 3. 명령어 조회 API (Polling)
def check_server_command():
    url = f"{SERVER_URL}/api/v1/pet/command/{DEVICE_ID}"
    try:
        res = requests.get(url, timeout=2)
        if res.status_code == 200:
            return res.json()
    except Exception:
        pass
    return None

# 4. 명령어 수신 확인 API (Ack)
def acknowledge_command(command_id):
    url = f"{SERVER_URL}/api/v1/pet/command/ack/{command_id}"
    try:
        res = requests.post(url, timeout=2)
        if res.status_code == 200:
            print(f"✉️ 명령어 [{command_id}] 수신 완료(Ack) 처리함.")
    except Exception as e:
        print(f"❌ 명령어 Ack 처리 실패: {e}")

# 5. 비전 이벤트 전송 API (밥그릇 안착)
def send_bowl_event():
    url = f"{SERVER_URL}/api/v1/device/vision-event"
    payload = {"deviceId": DEVICE_ID, "eventType": "FOOD_BOWL"}
    try:
        res = requests.post(url, json=payload, timeout=2)
        if res.status_code == 200:
            print("✅ [밥그릇 전송 완료] 백엔드 밥 주기 크로스 체크 자물쇠 완벽 해제!")
            return True
        else:
            print(f"⚠️ 백엔드 응답 에러 (Status: {res.status_code})")
    except Exception as e:
        print(f"❌ 로컬 서버 밥그릇 이벤트 전송 실패: {e}")
    return False

# 6. 제스쳐 이벤트 전송 API (훈련 제스쳐)
def send_gesture_event(gesture_type, confidence=1.0):
    url = f"{SERVER_URL}/api/v1/training/gesture"
    payload = {
        "deviceId": DEVICE_ID,
        "gestureType": gesture_type,
        "confidence": confidence
    }
    try:
        res = requests.post(url, json=payload, timeout=2)
        if res.status_code == 200:
            print(f"📡 [제스쳐 전송 완료] 제스쳐: {gesture_type} (신뢰도: {confidence*100:.1f}%)")
            return True
        else:
            print(f"⚠️ 백엔드 응답 에러 (Status: {res.status_code})")
    except Exception as e:
        print(f"❌ 로컬 서버 제스쳐 이벤트 전송 실패: {e}")
    return False

# 메인 루프
def main():
    print("🐾 [Pet-Ready Integrated Vision AI] 통합 이벤트 기반 비전 제어 스크립트 가동 (YOLOv8 + MediaPipe)...")
    print("   - 탐지 대상 1: 밥그릇 안착 감지 (YOLOv8 -> bowl, cup, frisbee)")
    print("   - 탐지 대상 2: 훈련 제스쳐 감지 (MediaPipe Hands -> SIT[검지], STAY[보자기])")
    print(f"📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
    
    # YOLOv8 모델 로드
    try:
        model = YOLO(MODEL_PATH)
    except Exception as e:
        print(f"❌ YOLO 모델 로드 실패: {e}")
        return

    # MediaPipe Hands 모델 초기화
    hands = None
    mp_draw = None
    mp_hands = None
    if mp is not None:
        mp_hands = mp.solutions.hands
        hands = mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            min_detection_confidence=0.7,
            min_tracking_confidence=0.7
        )
        mp_draw = mp.solutions.drawing_utils

    state = "DETECTING" # STANDBY or DETECTING
    
    # 카메라 즉시 초기화
    cap = initialize_camera()
    if cap is None or not cap.isOpened():
        print("❌ 카메라 장치를 열 수 없습니다. 종료합니다.")
        return
        
    # 밥그릇 상태 변수
    bowl_frame_counter = 0
    bowl_missed_counter = 0
    is_bowl_event_sent = False
    
    # 젯슨나노 연산 부하 완화를 위한 최적화 변수
    loop_count = 0
    bowl_detected_in_this_frame = False
    detected_box_coords = None
    detected_conf = 0.0
    detected_cls_name = "CONTAINER"
    
    # 제스쳐 상태 변수
    gesture_detect_type = "NONE"
    gesture_frame_counter = 0
    gesture_last_sent_time = 0
    gesture_sent_lock = "NONE"

    detect_start_time = time.time()
    detect_timeout_sec = 999999  # 무제한 대기
    
    # 백엔드 명령어 주기 조절을 위한 마지막 폴링 타임스탬프
    last_poll_time = 0
    poll_interval = 4.0 # STANDBY 상태 시 4초마다 폴링
    last_check_cancel_time = 0
    
    HEADLESS_MODE = False  # 실시간 모니터 창 활성화 (카메라 화면 표출)
    if not HEADLESS_MODE:
        cv2.namedWindow('Pet-Ready Integrated Local Vision AI Simulation', cv2.WINDOW_NORMAL)

    while True:
        current_time = time.time()
        
        # ----------------------------------------------------
        # [상태 1] STANDBY 모드 - 백엔드 명령 폴링 대기
        # ----------------------------------------------------
        if state == "STANDBY":
            if current_time - last_poll_time >= poll_interval:
                last_poll_time = current_time
                cmd_data = check_server_command()
                if cmd_data and cmd_data.get("hasCommand"):
                    cmd = cmd_data.get("command")
                    cmd_id = cmd_data.get("commandId")
                    duration = cmd_data.get("durationSec", 1800)
                    
                    if cmd == "START_VISION":
                        print(f"\n🚀 [명령어 수신] 비전 탐지 시작 명령 감지 (제한시간: {duration}초)")
                        acknowledge_command(cmd_id)
                        
                        # 카메라 초기화 시도
                        cap = initialize_camera()
                        if cap is None or not cap.isOpened():
                            print("❌ 카메라 장치를 열 수 없습니다. 다시 STANDBY로 돌아갑니다.")
                            state = "STANDBY"
                            continue
                        
                        # 상태 리셋 및 전환
                        state = "DETECTING"
                        bowl_frame_counter = 0
                        bowl_missed_counter = 0
                        is_bowl_event_sent = False
                        
                        gesture_detect_type = "NONE"
                        gesture_frame_counter = 0
                        gesture_last_sent_time = 0
                        gesture_sent_lock = "NONE"

                        detect_start_time = time.time()
                        detect_timeout_sec = duration if duration > 0 else 1800
                        print("📹 카메라 가동 및 실시간 밥그릇 & 제스처 동시 탐색 루프 진입...")
                
            # 슬립을 주어 CPU 점유율 방지
            time.sleep(0.5)
            
        # ----------------------------------------------------
        # [상태 2] DETECTING 모드 - 카메라 스트림 기반 YOLO & MediaPipe 동시 검출 수행
        # ----------------------------------------------------
        elif state == "DETECTING":
            # 1. 타임아웃 체크
            if time.time() - detect_start_time > detect_timeout_sec:
                print("⏳ 비전 탐색 시간 초과(Timeout) ➡️ 카메라를 끄고 대기 모드로 전환합니다.")
                if cap:
                    cap.release()
                if not HEADLESS_MODE:
                    cv2.destroyAllWindows()
                state = "STANDBY"
                print(f"\n📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
                continue
                
            # 2. 실시간 프레임 읽기
            ret, frame = cap.read()
            if not ret:
                print("❌ 카메라 프레임을 읽을 수 없습니다. 대기 모드로 돌아갑니다.")
                if cap:
                    cap.release()
                if not HEADLESS_MODE:
                    cv2.destroyAllWindows()
                state = "STANDBY"
                print(f"\n📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
                continue
                
            # 거울 모드 (좌우 반전) - 셀카 구도와 일치시켜 조작 편의성 보완
            frame = cv2.flip(frame, 1)
            h, w, _ = frame.shape

            # ----------------------------------------------------
            # [A] 밥그릇 감지 알고리즘 (YOLOv8) - 6프레임당 1회만 연산하도록 스킵하여 렉 제거
            # ----------------------------------------------------
            loop_count += 1
            if loop_count % 6 == 0:
                results = model(frame, verbose=False)[0]
                bowl_detected_in_this_frame = False
                detected_box_coords = None
                detected_conf = 0.0
                detected_cls_name = "CONTAINER"
                
                for box in results.boxes:
                    cls = int(box.cls[0])
                    conf = float(box.conf[0])
                    
                    # COCO 29: frisbee (PLATE), 41: cup (CUP), 45: bowl (BOWL)
                    if cls in [29, 41, 45] and conf >= CONFIDENCE_THRES:
                        bowl_detected_in_this_frame = True
                        detected_box_coords = box.xyxy[0]
                        detected_conf = conf
                        if cls == 29:
                            detected_cls_name = "PLATE"
                        elif cls == 41:
                            detected_cls_name = "CUP"
                        else:
                            detected_cls_name = "BOWL"
                        break
                    
            # 밥그릇 디바운싱 통제 및 전송
            if bowl_detected_in_this_frame:
                bowl_missed_counter = 0
                if not is_bowl_event_sent:
                    bowl_frame_counter += 1
                    
                    # 밥그릇 감지 게이지 렌더링
                    cv2.putText(frame, f"Bowl Locking... {bowl_frame_counter}/{REQUIRED_FRAMES_BOWL}", (30, 50),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 165, 255), 2)
                    progress_w = int((w - 60) * (bowl_frame_counter / REQUIRED_FRAMES_BOWL))
                    cv2.line(frame, (30, h - 30), (30 + progress_w, h - 30), (0, 165, 255), 8)
                    cv2.rectangle(frame, (30, h - 35), (w - 30, h - 25), (200, 200, 200), 1)
                    
                    if bowl_frame_counter >= REQUIRED_FRAMES_BOWL:
                        print("🎯 [비전 확정] 밥그릇 안착 확인 ➡️ 백엔드로 이벤트 전송!")
                        if send_bowl_event():
                            is_bowl_event_sent = True
            else:
                if not is_bowl_event_sent:
                    bowl_missed_counter += 1
                    if bowl_missed_counter >= 8:
                        bowl_frame_counter = 0
                        bowl_missed_counter = 0
                else:
                    bowl_missed_counter += 1
                    if bowl_missed_counter >= 15: # 완전히 사라지면 리셋하고 다음 감지 준비
                        print("🍃 밥그릇 이탈 감지 ➡️ 다음 감지를 위해 리셋.")
                        is_bowl_event_sent = False
                        bowl_frame_counter = 0
                        bowl_missed_counter = 0

            # 밥그릇 바운딩 박스 그리기
            if bowl_detected_in_this_frame and detected_box_coords is not None:
                x1, y1, x2, y2 = map(int, detected_box_coords)
                box_color = (0, 255, 0) if is_bowl_event_sent else (255, 255, 0)
                cv2.rectangle(frame, (x1, y1), (x2, y2), box_color, 3)
                cv2.putText(frame, f"{detected_cls_name} ({detected_conf*100:.1f}%)", (x1, y1 - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, box_color, 2)

            # ----------------------------------------------------
            # [B] 제스처 감지 알고리즘 (MediaPipe Hands)
            # ----------------------------------------------------
            detected_gesture = "NONE"
            hand_confidence = 0.0

            if hands is not None:
                rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                hand_results = hands.process(rgb_frame)
                
                if hand_results.multi_hand_landmarks and hand_results.multi_handedness:
                    for hand_landmarks, handedness in zip(hand_results.multi_hand_landmarks, hand_results.multi_handedness):
                        hand_confidence = handedness.classification[0].score
                        
                        # 랜드마크 연결 스켈레톤 그리기
                        mp_draw.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)
                        
                        landmarks = hand_landmarks.landmark
                        
                        # 펴짐 여부 판정 (Y좌표: 팁 < PIP 마디 일 때 펴짐)
                        is_index_open = landmarks[8].y < landmarks[6].y
                        is_middle_open = landmarks[12].y < landmarks[10].y
                        is_ring_open = landmarks[16].y < landmarks[14].y
                        is_pinky_open = landmarks[20].y < landmarks[18].y

                        # SIT(앉아): 검지손가락 하나만 펼치고 중지/약지/새끼는 굽힌 상태 (가리키기 지시)
                        if is_index_open and not is_middle_open and not is_ring_open and not is_pinky_open:
                            detected_gesture = "SIT"
                        # STAY(기다려): 네 손가락 이상 완전히 쫙 편 보자기 손바닥 상태
                        elif is_index_open and is_middle_open and is_ring_open and is_pinky_open:
                            detected_gesture = "STAY"

            # 제스처 쿨다운 락 판단
            if current_time - gesture_last_sent_time > GESTURE_COOLDOWN_SEC:
                gesture_sent_lock = "NONE"

            # 제스처 디바운싱 통제 및 전송
            if detected_gesture != "NONE" and detected_gesture != gesture_sent_lock:
                if gesture_detect_type == detected_gesture:
                    gesture_frame_counter += 1
                else:
                    gesture_detect_type = detected_gesture
                    gesture_frame_counter = 1
                
                # 제스쳐 감지 게이지 렌더링
                cv2.putText(frame, f"Gesture Locking... {gesture_frame_counter}/{REQUIRED_FRAMES_GESTURE}", (30, 90),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 165, 255), 2)
                
                if gesture_frame_counter >= REQUIRED_FRAMES_GESTURE:
                    print(f"🎯 [제스쳐 확정] {detected_gesture} 감지 ➡️ 백엔드로 전송!")
                    if send_gesture_event(detected_gesture, hand_confidence):
                        gesture_last_sent_time = current_time
                        gesture_sent_lock = detected_gesture
                    gesture_frame_counter = 0
                    gesture_detect_type = "NONE"
            else:
                gesture_frame_counter = 0
                gesture_detect_type = "NONE"

            # ----------------------------------------------------
            # [C] 화면 오버레이 및 상태 정보 그리기
            # ----------------------------------------------------
            # 밥그릇 해제 성공 테두리 렌더링
            if is_bowl_event_sent:
                cv2.rectangle(frame, (0, 0), (w, h), (0, 255, 0), 10)
                cv2.putText(frame, "BOWL: UNLOCKED", (30, h - 80), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            else:
                cv2.putText(frame, "BOWL: LOCKED", (30, h - 80), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)

            # 제스쳐 락아웃 쿨다운 텍스트 렌더링
            if gesture_sent_lock != "NONE":
                remaining = int(GESTURE_COOLDOWN_SEC - (current_time - gesture_last_sent_time))
                cv2.putText(frame, f"SENT {gesture_sent_lock} (COOLDOWN {remaining}s)", (30, h - 50),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            else:
                cv2.putText(frame, "GESTURE: READY", (30, h - 50), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 0), 2)

            if not HEADLESS_MODE:
                cv2.imshow('Pet-Ready Integrated Local Vision AI Simulation', frame)
            
            # 탐지 모드 진행 중 강제 취소 명령(STOP_VISION) 확인 (3초마다 체크)
            if current_time - last_check_cancel_time >= 3.0:
                last_check_cancel_time = current_time
                cmd_data = check_server_command()
                if cmd_data and cmd_data.get("hasCommand"):
                    cmd = cmd_data.get("command")
                    cmd_id = cmd_data.get("commandId")
                    if cmd == "STOP_VISION":
                        print("🛑 [명령어 수신] 비전 탐지 강제 취소(STOP_VISION) 감지 ➡️ 카메라를 끕니다.")
                        acknowledge_command(cmd_id)
                        if cap:
                            cap.release()
                        if not HEADLESS_MODE:
                            cv2.destroyAllWindows()
                        state = "STANDBY"
                        print(f"\n📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
                        continue
            
            # 젯슨나노 과열 방지 및 CPU 렉 완화를 위한 미세 딜레이 (약 60FPS 상한 제약)
            time.sleep(0.015)

            # 'q' 키 누르면 수동 종료
            if not HEADLESS_MODE:
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    print("🛑 사용자에 의한 수동 종료.")
                    if cap:
                        cap.release()
                    cv2.destroyAllWindows()
                    break
            else:
                # 헤드리스용 미세 슬립 보완
                time.sleep(0.01)
 
if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n🐾 시스템 키보드 중단으로 안전 종료.")
