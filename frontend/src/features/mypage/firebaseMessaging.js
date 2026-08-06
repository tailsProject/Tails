// Firebase Cloud Messaging 웹 푸시 토큰 발급
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

export function isPushConfigured() {
  return Boolean(firebaseConfig.apiKey && vapidKey);
}

// FCM 전용 서비스워커 등록, public/firebase-messaging-sw.js 사용
async function registerServiceWorker() {
  const query = new URLSearchParams(firebaseConfig).toString();
  await navigator.serviceWorker.register(`/firebase-messaging-sw.js?${query}`);
  return navigator.serviceWorker.ready;
}

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

  onMessage(messaging, (payload) => {
    console.info('[FCM] foreground message', payload);
  });

  return getToken(messaging, { vapidKey, serviceWorkerRegistration: registration });
}
