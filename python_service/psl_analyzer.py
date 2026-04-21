import cv2
import numpy as np
import mediapipe as mp
from flask import Flask, request, jsonify
import urllib.request
import os
import math
import tempfile

app = Flask(__name__)

# Правильная инициализация MediaPipe (для версии 0.10.30+)
mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=1,
    refine_landmarks=True,
    min_detection_confidence=0.5
)

# Индексы ключевых точек (468 точек)
LM = {
    'left_cheek': 234,
    'right_cheek': 454,
    'chin': 152,
    'forehead': 10,
    'left_eye_left': 33,
    'left_eye_right': 133,
    'right_eye_left': 362,
    'right_eye_right': 263,
    'lips_left': 61,
    'lips_right': 291,
    'left_jaw': 172,
    'right_jaw': 396,
}

def calculate_symmetry(landmarks):
    """Симметрия лица (0-1)"""
    left_cheek = landmarks[LM['left_cheek']]
    right_cheek = landmarks[LM['right_cheek']]
    left_eye = landmarks[LM['left_eye_left']]
    right_eye = landmarks[LM['right_eye_left']]
    
    cheek_sym = 1.0 - min(1.0, abs(left_cheek.x - right_cheek.x) * 2)
    eye_h_sym = 1.0 - min(1.0, abs(left_eye.x - right_eye.x) * 2)
    eye_v_sym = 1.0 - min(1.0, abs(left_eye.y - right_eye.y) * 3)
    
    return (cheek_sym + eye_h_sym + eye_v_sym) / 3.0

def calculate_proportions(landmarks):
    """Пропорции лица (0-10)"""
    face_top = landmarks[LM['forehead']].y
    face_bottom = landmarks[LM['chin']].y
    face_height = face_bottom - face_top
    
    face_left = landmarks[LM['left_cheek']].x
    face_right = landmarks[LM['right_cheek']].x
    face_width = face_right - face_left
    
    if face_width <= 0:
        face_width = 0.1
    
    face_ratio = face_height / face_width
    ratio_score = 10.0 - min(9.0, abs(face_ratio - 1.55) * 12)
    
    left_eye = landmarks[LM['left_eye_left']].x
    right_eye = landmarks[LM['right_eye_left']].x
    interocular = abs(right_eye - left_eye)
    interocular_ratio = interocular / face_width
    eye_score = 10.0 - min(9.0, abs(interocular_ratio - 0.46) * 20)
    
    lips_left = landmarks[LM['lips_left']].x
    lips_right = landmarks[LM['lips_right']].x
    mouth_width = abs(lips_right - lips_left)
    mouth_ratio = mouth_width / face_width
    mouth_score = 10.0 - min(9.0, abs(mouth_ratio - 0.35) * 18)
    
    return (ratio_score + eye_score + mouth_score) / 3.0

def calculate_bone_structure(landmarks, gender):
    """Костная структура (0-10)"""
    left_eye_inner = landmarks[LM['left_eye_left']].y
    left_eye_outer = landmarks[LM['left_eye_right']].y
    canthal_tilt = (left_eye_outer - left_eye_inner) * 100
    tilt_score = 10.0 - min(9.0, abs(canthal_tilt - 4.0) * 1.5)
    
    target_jaw = 0.85 if gender == 'male' else 0.75
    jaw_score = 10.0 - min(9.0, abs(0.85 - target_jaw) * 15)
    
    left_cheek = landmarks[LM['left_cheek']].x
    right_cheek = landmarks[LM['right_cheek']].x
    cheek_width = abs(right_cheek - left_cheek)
    
    left_jaw = landmarks[LM['left_jaw']].x
    right_jaw = landmarks[LM['right_jaw']].x
    jaw_width = abs(right_jaw - left_jaw)
    
    jaw_to_cheek = jaw_width / cheek_width if cheek_width > 0 else 1.0
    jaw_shape_score = 10.0 - min(9.0, abs(jaw_to_cheek - 0.9) * 12)
    
    return (tilt_score + jaw_score + jaw_shape_score) / 3.0

def estimate_gender(landmarks):
    """Определение пола"""
    face_top = landmarks[LM['forehead']].y
    face_bottom = landmarks[LM['chin']].y
    face_height = face_bottom - face_top
    
    face_left = landmarks[LM['left_cheek']].x
    face_right = landmarks[LM['right_cheek']].x
    face_width = face_right - face_left
    
    if face_width > 0:
        ratio = face_height / face_width
        return 'male' if ratio < 1.52 else 'female'
    return 'unknown'

def analyze_face(image_path, user_gender):
    img = cv2.imread(image_path)
    if img is None:
        return None
    
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    results = face_mesh.process(img_rgb)
    
    if not results.multi_face_landmarks:
        return None
    
    landmarks = results.multi_face_landmarks[0].landmark
    
    symmetry = calculate_symmetry(landmarks)
    proportions = calculate_proportions(landmarks)
    bone = calculate_bone_structure(landmarks, user_gender)
    
    rating = (symmetry * 3.0) + (proportions * 0.4) + (bone * 0.3)
    rating = max(1.0, min(10.0, rating))
    
    detected_gender = estimate_gender(landmarks)
    
    if user_gender and user_gender != 'unknown' and detected_gender != 'unknown':
        if detected_gender != user_gender:
            rating = rating * 0.85
        else:
            rating = min(10.0, rating * 1.05)
    
    return {
        'rating': round(rating, 2),
        'symmetry': round(symmetry, 3),
        'proportions': round(proportions, 2),
        'bone': round(bone, 2),
        'detected_gender': detected_gender
    }

@app.route('/analyze', methods=['POST'])
def analyze():
    data = request.get_json()
    photo_url = data.get('photo_url')
    gender = data.get('gender', 'unknown')
    
    if not photo_url:
        return jsonify({'error': 'no photo_url'}), 400
    
    with tempfile.NamedTemporaryFile(suffix='.jpg', delete=False) as tmp:
        tmp_path = tmp.name
    
    try:
        urllib.request.urlretrieve(photo_url, tmp_path)
        result = analyze_face(tmp_path, gender)
        os.unlink(tmp_path)
        
        if result is None:
            return jsonify({'error': 'face not detected'}), 400
        
        return jsonify(result)
        
    except Exception as e:
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok'})

if __name__ == '__main__':
    print('🚀 PSL Analyzer запущен на порту 5001')
    app.run(host='0.0.0.0', port=5001, debug=False)
