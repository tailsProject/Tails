// 내가 쓴 게시글 목록 페이지
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyBoards } from './api';
import Pagination from '../../components/Pagination/Pagination';
import { resolveImage } from '../../utils/resolveImage';
import { PencilIcon, EyeIcon, HeartIcon, ChatBubbleIcon } from '../../components/Icon/Icon';
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
                  {resolveImage(board.thumbnailUrl) ? (
                    <img className={styles.itemThumb} src={resolveImage(board.thumbnailUrl)} alt="" />
                  ) : (
                    <span className={styles.itemThumb}>
                      <PencilIcon />
                    </span>
                  )}
                  <span className={styles.itemBody}>
                    <span className={styles.title}>
                      {board.status === 'DRAFT' && <span className={`${styles.badge} ${styles.badgeDraft}`}>임시저장</span>}
                      {board.title}
                    </span>
                    {board.excerpt && <span className={styles.excerpt}>{board.excerpt}</span>}
                    <span className={styles.metaRow}>
                      <span className={styles.metaItem}>
                        <EyeIcon /> {board.viewCount}
                      </span>
                      <span className={styles.metaItem}>
                        <HeartIcon /> {board.likeCount}
                      </span>
                      <span className={styles.metaItem}>
                        <ChatBubbleIcon /> {board.commentCount}
                      </span>
                      <span className={styles.metaDate}>{new Date(board.createdAt).toLocaleDateString()}</span>
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          {boardPage.content.length === 0 && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}>
                <PencilIcon />
              </span>
              <p>작성한 글이 없습니다.</p>
            </div>
          )}
          <Pagination page={boardPage.number} totalPages={boardPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
