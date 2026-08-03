// 브라우저가 꺼져있거나 탭이 백그라운드일 때 푸시를 받는 서비스워커.
// Vite는 public/ 파일에 환경변수를 주입하지 않으므로, 등록 시 URL 쿼리스트링으로
// firebaseConfig를 넘겨받는다 (src/features/mypage/firebaseMessaging.js 참고).
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js');

const params = new URLSearchParams(self.location.search);
const firebaseConfig = {
  apiKey: params.get('apiKey'),
  authDomain: params.get('authDomain'),
  projectId: params.get('projectId'),
  storageBucket: params.get('storageBucket'),
  messagingSenderId: params.get('messagingSenderId'),
  appId: params.get('appId'),
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// 탭이 백그라운드일 때 도착한 알림을 브라우저 알림으로 띄운다
messaging.onBackgroundMessage((payload) => {
  const { title, body } = payload.notification ?? {};
  if (!title) return;
  self.registration.showNotification(title, { body, icon: '/favicon.svg' });
});
