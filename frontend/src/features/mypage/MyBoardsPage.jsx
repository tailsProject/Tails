import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyBoards } from './api';
import Pagination from '../../components/Pagination/Pagination';
import styles from './ListPage.module.scss';

export default function MyBoardsPage() {
  const [page, setPage] = useState(0);
  const [boardPage, setBoardPage] = useState(null);

  useEffect(() => {
    getMyBoards({ page }).then((res) => setBoardPage(res.data.data));
  }, [page]);

  return (
    <div>
      <div className={styles.header}>
        <h1>내가 쓴 글</h1>
      </div>

      {boardPage && (
        <>
          <ul className={styles.list}>
            {boardPage.content.map((board) => (
              <li key={board.boardId}>
                <Link to={`/boards/${board.boardId}`} className={styles.item}>
                  <span className={styles.title}>
                    {board.status === 'DRAFT' && <span className={`${styles.badge} ${styles.badgeDraft}`}>임시저장</span>}
                    {board.title}
                  </span>
                  <span className={styles.meta}>
                    조회 {board.viewCount} · 좋아요 {board.likeCount} ·{' '}
                    {new Date(board.createdAt).toLocaleDateString()}
                  </span>
                </Link>
              </li>
            ))}
            {boardPage.content.length === 0 && <p className={styles.empty}>작성한 글이 없습니다.</p>}
          </ul>
          <Pagination page={boardPage.number} totalPages={boardPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
