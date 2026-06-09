import cv2
import requests
import time
from ultralytics import YOLO
import numpy as np

# 1. 로컬 환경 설정 변수
MODEL_PATH = 'yolov8n.pt'
SERVER_URL = 'http://localhost:8080/api/v1/device/vision-event'  # 👈 로컬호스트 통합 검증 주소
DEVICE_ID = 'DOG_01'
REQUIRED_FRAMES = 15  # 15프레임으로 단축
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

try:
    model = YOLO(MODEL_PATH)
    cap = initialize_camera()
    if cap is None or not cap.isOpened():
        raise Exception("활성화된 카메라 장치를 찾을 수 없거나 macOS 카메라 접근 권한이 없습니다.")
except Exception as e:
    print(f"❌ 초기화 실패: {e}")
    exit()

bowl_frame_counter = 0
missed_frame_counter = 0  # 찰나의 프레임 미감지 드랍 방어 버퍼용 카운터
is_event_sent = False

print("🐾 [Pet-Ready Local Vision AI] 상시 실행형 밥그릇 탐지 시스템 가동 시작 (테스트용)...")

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        print("❌ 카메라 프레임을 읽을 수 없습니다.")
        break

    # YOLOv8 추론 수행 (verbose=False로 콘솔 과부하 방지)
    results = model(frame, verbose=False)[0]
    bowl_detected_in_this_frame = False
    detected_box_coords = None
    detected_conf = 0.0
    detected_cls_name = "CONTAINER"
    
    for box in results.boxes:
        cls = int(box.cls[0])
        conf = float(box.conf[0])
        
        # COCO 클래스 29 = frisbee (접시 대용), 41 = cup, 45 = bowl
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

    # 밥그릇/컵/접시 검출 시 실시간 Bounding Box 선 렌더링
    if bowl_detected_in_this_frame and detected_box_coords is not None:
        x1, y1, x2, y2 = map(int, detected_box_coords)
        box_color = (0, 255, 0) if is_event_sent else (255, 255, 0)
        cv2.rectangle(frame, (x1, y1), (x2, y2), box_color, 3)
        cv2.putText(frame, f"{detected_cls_name} DETECTED ({detected_conf*100:.1f}%)", (x1, y1 - 10), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, box_color, 2)

    # 디바운싱 및 상태 머신 흐름 통제
    if bowl_detected_in_this_frame:
        missed_frame_counter = 0
        
        if not is_event_sent:
            bowl_frame_counter += 1
            
            # 실시간 카운트다운 텍스트 렌더링
            cv2.putText(frame, f"Locking... {bowl_frame_counter}/{REQUIRED_FRAMES}", (30, 50), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 165, 255), 2)
            
            # 화면 하단에 가로 막대 프로그레스 바 선(Line) 시각화 (두께 10px)
            h, w, _ = frame.shape
            progress_w = int((w - 60) * (bowl_frame_counter / REQUIRED_FRAMES))
            cv2.line(frame, (30, h - 30), (30 + progress_w, h - 30), (0, 165, 255), 10)
            cv2.rectangle(frame, (30, h - 35), (w - 30, h - 25), (200, 200, 200), 1)  # 전체 게이지 아웃라인 선
            
            if bowl_frame_counter >= REQUIRED_FRAMES:
                print(f"🎯 [비전 확정] 밥그릇/컵/접시 안착 완료 ➡️ 로컬 서버로 신호 발사!")
                try:
                    payload = {
                        "deviceId": DEVICE_ID,
                        "eventType": "FOOD_BOWL"
                    }
                    res = requests.post(SERVER_URL, json=payload, timeout=2)
                    
                    if res.status_code == 200:
                        print("✅ 백엔드 2중 크로스 체크 자물쇠 완벽 해제 완료!")
                        is_event_sent = True
                    else:
                        print(f"⚠️ 백엔드 응답 에러 (Status: {res.status_code})")
                except Exception as e:
                    print(f"❌ 로컬 서버 통신 실패 (스프링부트 가동 상태 확인 필수): {e}")
    else:
        if not is_event_sent:
            missed_frame_counter += 1
            if missed_frame_counter >= 8:
                bowl_frame_counter = 0
                missed_frame_counter = 0
        else:
            missed_frame_counter += 1
            if missed_frame_counter >= 15:  # 완전히 사라진 경우에만 리셋하여 플리커링 방지
                print("🍃 밥그릇/컵 이탈 감지 ➡️ 비전 센서 상태 초기화.")
                is_event_sent = False
                bowl_frame_counter = 0
                missed_frame_counter = 0

    # 락 해제 성공 시 화면 전체 테두리에 굵은 초록색 테두리 선 렌더링
    if is_event_sent:
        h, w, _ = frame.shape
        cv2.rectangle(frame, (0, 0), (w, h), (0, 255, 0), 12)

    # 시연용 실시간 상태 UI 텍스트 출력
    status_text = "STATUS: BOWL_DETECTED (LOCK UNLOCKED)" if is_event_sent else "STATUS: WAITING_FOR_BOWL"
    status_color = (0, 255, 0) if is_event_sent else (0, 0, 255)
    cv2.putText(frame, status_text, (30, 90), cv2.FONT_HERSHEY_SIMPLEX, 0.7, status_color, 2)

    cv2.imshow('Pet-Ready Local Vision AI Simulation (Always)', frame)
    
    # 'q' 키 누르면 안전하게 가동 종료
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
print("🐾 시스템 안전하게 종료 완료.")
