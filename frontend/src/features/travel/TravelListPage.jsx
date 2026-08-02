// 내 여행 일정 목록 페이지
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getMyTravels } from './api';
import Pagination from '../../components/Pagination/Pagination';
import TravelFormModal from './TravelFormModal';
import { SuitcaseIcon, PlusIcon } from '../../components/Icon/Icon';
import Button from '../../components/Button/Button';
import styles from './TravelListPage.module.scss';

export default function TravelListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [travelPage, setTravelPage] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    getMyTravels({ page }).then((res) => setTravelPage(res.data.data));
  }, [page]);

  function handleCreated(travelId) {
    setModalOpen(false);
    navigate(`/travels/${travelId}`);
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <h1>여행일정</h1>
        <Button onClick={() => setModalOpen(true)}>
          <PlusIcon /> 새 여행 만들기
        </Button>
      </div>

      <TravelFormModal open={modalOpen} onClose={() => setModalOpen(false)} onSaved={handleCreated} />

      {travelPage && (
        <>
          <ul className={styles.grid}>
            {travelPage.content.map((travel) => (
              <li key={travel.travelId}>
                <Link to={`/travels/${travel.travelId}`} className={styles.card}>
                  <span className={styles.cardIcon}>
                    <SuitcaseIcon />
                  </span>
                  <div className={styles.cardBody}>
                    <p className={styles.title}>{travel.title}</p>
                    <p className={styles.meta}>
                      {travel.startDate} ~ {travel.endDate}
                    </p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          {travelPage.content.length === 0 && (
            <div className={styles.emptyState}>
              <p className={styles.emptyIcon}>
                <SuitcaseIcon />
              </p>
              <p>아직 만든 여행 일정이 없어요.</p>
              <button type="button" className={styles.newButton} onClick={() => setModalOpen(true)}>
                <PlusIcon /> 첫 여행 만들어보기
              </button>
            </div>
          )}
          <Pagination page={travelPage.number} totalPages={travelPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
