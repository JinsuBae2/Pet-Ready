import cv2
import requests
import time
from ultralytics import YOLO
import numpy as np

# 1. 로컬 환경 설정 변수
MODEL_PATH = 'yolov8n.pt'
SERVER_URL = 'http://localhost:8080'  # 백엔드 서버 베이스 주소
DEVICE_ID = 'DOG_01'
REQUIRED_FRAMES = 15  # 안착 판정 프레임 수
CONFIDENCE_THRES = 0.35  # 접시 감도 조절을 위해 0.35로 완화

# 2. 카메라 하드웨어 초기화 함수
def initialize_camera():
    backends = [cv2.CAP_AVFOUNDATION, cv2.CAP_ANY]
    for index in range(5):
        for backend in backends:
            try:
                cap = cv2.VideoCapture(index, backend)
                if cap.isOpened():
                    ret, test_frame = cap.read()
                    if ret and test_frame is not None:
                        mean_brightness = np.mean(test_frame)
                        if mean_brightness > 2.0:
                            print(f"✅ 카메라 장치 인덱스 [{index}] (백엔드: {backend}, 밝기: {mean_brightness:.2f}) 연결 성공!")
                            return cap
                    cap.release()
            except Exception:
                continue
    print("⚠️ 유효한 밝기의 실제 카메라를 탐색하지 못했습니다. 기본 장치(0번) 연결을 재시도합니다.")
    return cv2.VideoCapture(0, cv2.CAP_AVFOUNDATION)

# 3. 명령어 조회 API (Polling)
def check_server_command():
    url = f"{SERVER_URL}/api/v1/pet/command/{DEVICE_ID}"
    try:
        res = requests.get(url, timeout=2)
        if res.status_code == 200:
            return res.json()
    except Exception:
        # 서버 미기동 시 콘솔이 도배되는 로그 폭발 방지
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

# 5. 비전 이벤트 전송 API
def send_vision_event():
    url = f"{SERVER_URL}/api/v1/device/vision-event"
    payload = {"deviceId": DEVICE_ID, "eventType": "FOOD_BOWL"}
    try:
        res = requests.post(url, json=payload, timeout=2)
        if res.status_code == 200:
            print("✅ 백엔드 2중 크로스 체크 자물쇠 완벽 해제 완료!")
            return True
        else:
            print(f"⚠️ 백엔드 응답 에러 (Status: {res.status_code})")
    except Exception as e:
        print(f"❌ 로컬 서버 통신 실패 (스프링부트 가동 상태 확인 필수): {e}")
    return False

# 메인 루프
def main():
    print("🐾 [Pet-Ready Local Vision AI] 이벤트 기반 비전 제어 스크립트 가동 (실전용)...")
    print(f"📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
    
    # YOLOv8 모델 미리 메모리에 로드
    try:
        model = YOLO(MODEL_PATH)
    except Exception as e:
        print(f"❌ YOLO 모델 로드 실패: {e}")
        return

    state = "STANDBY" # STANDBY or DETECTING
    cap = None
    
    # 상태별 변수
    bowl_frame_counter = 0
    missed_frame_counter = 0
    is_event_sent = False
    detect_start_time = 0
    detect_timeout_sec = 1800  # 기본 30분
    
    # 백엔드 명령어 주기 조절을 위한 마지막 폴링 타임스탬프
    last_poll_time = 0
    poll_interval = 4.0 # STANDBY 상태 시 4초마다 폴링
    last_check_cancel_time = 0
    
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
                        missed_frame_counter = 0
                        is_event_sent = False
                        detect_start_time = time.time()
                        detect_timeout_sec = duration if duration > 0 else 1800
                        print("📹 카메라 가동 및 실시간 밥그릇 탐색 루프 진입...")
                
            # 슬립을 주어 CPU 점유율 방지
            time.sleep(0.5)
            
        # ----------------------------------------------------
        # [상태 2] DETECTING 모드 - 카메라 스트림 기반 YOLO 검출 수행
        # ----------------------------------------------------
        elif state == "DETECTING":
            # 1. 타임아웃 체크 (30분 경과 시 자동 비전 오프)
            if time.time() - detect_start_time > detect_timeout_sec:
                print("⏳ 비전 탐색 시간 초과(Timeout) ➡️ 카메라를 끄고 대기 모드로 전환합니다.")
                if cap:
                    cap.release()
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
                cv2.destroyAllWindows()
                state = "STANDBY"
                print(f"\n📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
                continue
                
            # YOLOv8 추론 수행 (verbose=False로 콘솔 과부하 방지)
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
                    
            # 바운딩 박스 렌더링
            if bowl_detected_in_this_frame and detected_box_coords is not None:
                x1, y1, x2, y2 = map(int, detected_box_coords)
                box_color = (0, 255, 0) if is_event_sent else (255, 255, 0)
                cv2.rectangle(frame, (x1, y1), (x2, y2), box_color, 3)
                cv2.putText(frame, f"{detected_cls_name} DETECTED ({detected_conf*100:.1f}%)", (x1, y1 - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, box_color, 2)
                            
            # 디바운싱 및 상태 통제
            if bowl_detected_in_this_frame:
                missed_frame_counter = 0
                if not is_event_sent:
                    bowl_frame_counter += 1
                    
                    # 실시간 카운트다운 및 게이지 출력
                    cv2.putText(frame, f"Locking... {bowl_frame_counter}/{REQUIRED_FRAMES}", (30, 50),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 165, 255), 2)
                    h, w, _ = frame.shape
                    progress_w = int((w - 60) * (bowl_frame_counter / REQUIRED_FRAMES))
                    cv2.line(frame, (30, h - 30), (30 + progress_w, h - 30), (0, 165, 255), 10)
                    cv2.rectangle(frame, (30, h - 35), (w - 30, h - 25), (200, 200, 200), 1)
                    
                    if bowl_frame_counter >= REQUIRED_FRAMES:
                        print("🎯 [비전 확정] 밥그릇/컵 안착 확인 ➡️ 백엔드로 이벤트 발사!")
                        if send_vision_event():
                            is_event_sent = True
            else:
                if not is_event_sent:
                    missed_frame_counter += 1
                    if missed_frame_counter >= 8:
                        bowl_frame_counter = 0
                        missed_frame_counter = 0
                else:
                    missed_frame_counter += 1
                    if missed_frame_counter >= 15: # 완전히 사라지면 리셋하고 슬립 복귀 준비
                        print("🍃 밥그릇/컵 이탈 감지 ➡️ 자물쇠 해제 완료 상태 확인 및 카메라 종료.")
                        if cap:
                            cap.release()
                        cv2.destroyAllWindows()
                        state = "STANDBY"
                        print(f"\n📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
                        continue
                        
            # 자물쇠 해제 성공 테두리 렌더링
            if is_event_sent:
                h, w, _ = frame.shape
                cv2.rectangle(frame, (0, 0), (w, h), (0, 255, 0), 12)
                
            status_text = "STATUS: BOWL_DETECTED (UNLOCKED)" if is_event_sent else "STATUS: WAITING_FOR_BOWL"
            status_color = (0, 255, 0) if is_event_sent else (0, 0, 255)
            cv2.putText(frame, status_text, (30, 90), cv2.FONT_HERSHEY_SIMPLEX, 0.7, status_color, 2)
            
            cv2.imshow('Pet-Ready Jetson Local Vision AI Simulation (Event)', frame)
            
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
                        cv2.destroyAllWindows()
                        state = "STANDBY"
                        print(f"\n📡 백엔드 서버 ({SERVER_URL}) 연결 대기 중 (상태: STANDBY)...")
                        continue
            
            # 'q' 키 누르면 수동 종료
            if cv2.waitKey(1) & 0xFF == ord('q'):
                print("🛑 사용자에 의한 수동 종료.")
                if cap:
                    cap.release()
                cv2.destroyAllWindows()
                break

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n🐾 시스템 키보드 중단으로 안전 종료.")
