// 전체 페이지 뼈대. 헤더와 푸터 사이에 각 라우트 화면 렌더링
import { Outlet, ScrollRestoration, useLocation } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import styles from './Layout.module.scss';

export default function Layout() {
  const { pathname } = useLocation();
  // 메인/지도 페이지는 좌우 여백 없이 전체 폭으로 표시
  const isFullBleed = pathname === '/' || pathname === '/places';

  return (
    <div className={styles.wrapper}>
      <Header />
      <main className={isFullBleed ? styles.contentFullBleed : styles.content}>
        <Outlet />
      </main>
      <Footer />
      <ScrollRestoration />
    </div>
  );
}
