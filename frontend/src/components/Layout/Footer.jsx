// 전역 푸터. 이용약관/개인정보처리방침 모달 함께 관리
import { useState } from 'react';
import { Link } from 'react-router-dom';
import Modal from '../Modal/Modal';
import Button from '../Button/Button';
import { TERMS_CONTENT } from '../../features/auth/termsContent';
import styles from './Footer.module.scss';

const SUPPORT_EMAIL = 'tails@mail.com';

export default function Footer() {
  const [openTerms, setOpenTerms] = useState(null); 

  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div className={styles.brand}>
          <img src="/TailsLogo.png" alt="Tails" className={styles.logoImage} />
          <p>반려동물과 함께하는 여행 정보를 찾고, 일정을 만들고, 기록을 나누는 서비스.</p>
          <nav className={styles.links}>
            <Link to="/places">지도 탐색</Link>
            <Link to="/boards">피드</Link>
            <Link to="/travels">여행 일정</Link>
            <Link to="/mypage">마이페이지</Link>
          </nav>
        </div>
      </div>

      <div className={styles.bottomBar}>
        <p>
          © {new Date().getFullYear()} Tails. All rights reserved. · 문의: <a href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
        </p>
        <div className={styles.legalLinks}>
          <button type="button" onClick={() => setOpenTerms('terms')}>
            이용약관
          </button>
          <button type="button" onClick={() => setOpenTerms('privacy')}>
            개인정보처리방침
          </button>
        </div>
      </div>

      <Modal open={openTerms !== null} onClose={() => setOpenTerms(null)}>
        {openTerms && (
          <div className={styles.legalModal}>
            <h2>{TERMS_CONTENT[openTerms].title}</h2>
            <p className={styles.legalBody}>{TERMS_CONTENT[openTerms].body}</p>
            <Button type="button" onClick={() => setOpenTerms(null)}>
              닫기
            </Button>
          </div>
        )}
      </Modal>
    </footer>
  );
}
