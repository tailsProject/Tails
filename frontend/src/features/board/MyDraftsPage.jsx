// 내가 임시저장한 게시글 목록 페이지
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyDrafts } from './api';
import Pagination from '../../components/Pagination/Pagination';
import { PencilIcon } from '../../components/Icon/Icon';
import styles from './BoardListPage.module.scss';

export default function MyDraftsPage() {
  const [page, setPage] = useState(0);
  const [draftPage, setDraftPage] = useState(null);

  useEffect(() => {
    getMyDrafts({ page }).then((res) => setDraftPage(res.data.data));
  }, [page]);

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <h1>내 임시저장</h1>
      </div>

      {draftPage && (
        <>
          <ul className={styles.list}>
            {draftPage.content.map((board) => (
              <li key={board.boardId}>
                <Link to={`/boards/${board.boardId}/edit`} className={styles.item}>
                  <div className={styles.itemBody}>
                    <span className={styles.title}>{board.title || '(제목 없음)'}</span>
                    <span className={styles.author}>{new Date(board.createdAt).toLocaleDateString()}</span>
                  </div>
                </Link>
              </li>
            ))}
            {draftPage.content.length === 0 && (
              <div className={styles.empty}>
                <p className={styles.emptyIcon}><PencilIcon /></p>
                <p>임시저장한 글이 없습니다.</p>
              </div>
            )}
          </ul>
          <Pagination page={draftPage.number} totalPages={draftPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
