import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;

// .env에 키를 안 채워두면 푸시 기능 자체를 조용히 비활성화한다(백엔드 FirebaseConfig와 동일한 정책).
export function isPushConfigured() {
  return Boolean(firebaseConfig.apiKey && vapidKey);
}

// 서비스워커는 Vite가 환경변수를 못 넣어주므로 등록 시 쿼리스트링으로 설정값을 전달한다.
// register()는 설치가 끝나기 전에도 resolve되므로, subscribe()가 "no active Service Worker"로
// 실패하지 않도록 ready(활성화 완료)까지 기다린 뒤 반환한다.
async function registerServiceWorker() {
  const query = new URLSearchParams(firebaseConfig).toString();
  await navigator.serviceWorker.register(`/firebase-messaging-sw.js?${query}`);
  return navigator.serviceWorker.ready;
}

// 알림 권한을 요청하고 FCM 토큰을 발급받는다. 거부/미지원/설정 누락이면 null을 반환한다.
export async function requestPushToken() {
  if (!isPushConfigured() || !('serviceWorker' in navigator) || !('Notification' in window)) {
    return null;
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    return null;
  }

  const app = initializeApp(firebaseConfig);
  const messaging = getMessaging(app);
  const registration = await registerServiceWorker();

  // 포그라운드(탭이 열려있는 상태)로 온 알림도 콘솔에 남긴다 - 실제 화면 표시는 기존 알림함(폴링)이 담당
  onMessage(messaging, (payload) => {
    console.info('[FCM] foreground message', payload);
  });

  return getToken(messaging, { vapidKey, serviceWorkerRegistration: registration });
}
