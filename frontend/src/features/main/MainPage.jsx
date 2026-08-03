// 메인페이지 골격 + 히어로 섹션. 인기 게시글/장소/리뷰 데이터 연동은 다음 커밋에서 진행
import { Link } from 'react-router-dom';
import { MapIcon, SuitcaseIcon } from '../../components/Icon/Icon';
import styles from './MainPage.module.scss';

const ENTRY_LINKS = [
  { to: '/places', label: '지도', description: '반려동물 동반 가능한 장소를 찾아보세요', icon: MapIcon },
  { to: '/travels', label: '여행일정', description: '나만의 여행 일정을 만들어보세요', icon: SuitcaseIcon },
];

export default function MainPage() {
  return (
    <div className={styles.main}>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <h1>반려동물과 함께하는 여행, Tails</h1>
          <p>동반 가능한 장소를 찾고, 일정을 짜고, 경험을 나눠보세요.</p>
        </div>
      </section>

      <section className={styles.links}>
        {ENTRY_LINKS.map((link) => (
          <Link key={link.to} to={link.to} className={styles.card}>
            <span className={styles.cardIcon}>
              <link.icon />
            </span>
            <h2>{link.label}</h2>
            <p>{link.description}</p>
          </Link>
        ))}
      </section>
    </div>
  );
}
