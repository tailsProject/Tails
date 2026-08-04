import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyBookmarkedBoards, getMyBookmarkedPlaces } from './api';
import Pagination from '../../components/Pagination/Pagination';
import { resolveImage } from '../../utils/resolveImage';
import { PencilIcon, MapPinIcon, EyeIcon, HeartIcon, ChatBubbleIcon } from '../../components/Icon/Icon';
import styles from './ListPage.module.scss';

export default function MyBookmarksPage() {
  const [tab, setTab] = useState('boards'); // 'boards' | 'places'
  const [page, setPage] = useState(0);
  const [resultPage, setResultPage] = useState(null);

  useEffect(() => {
    setPage(0);
  }, [tab]);

  useEffect(() => {
    const fetcher = tab === 'boards' ? getMyBookmarkedBoards : getMyBookmarkedPlaces;
    fetcher({ page }).then((res) => setResultPage(res.data.data));
  }, [tab, page]);

  return (
    <div>
      <div className={styles.header}>
        <h1>찜 / 북마크</h1>
      </div>

      <div className={styles.tabs}>
        <button className={tab === 'boards' ? styles.tabActive : ''} onClick={() => setTab('boards')}>
          찜한 게시글
        </button>
        <button className={tab === 'places' ? styles.tabActive : ''} onClick={() => setTab('places')}>
          찜한 장소
        </button>
      </div>

      {resultPage && (
        <>
          <ul className={styles.list}>
            {tab === 'boards'
              ? resultPage.content.map((board) => (
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
                        <span className={styles.title}>{board.title}</span>
                        <span className={styles.metaRow}>
                          <span className={styles.metaItem}>{board.authorNickname}</span>
                          <span className={styles.metaItem}>
                            <HeartIcon /> {board.likeCount}
                          </span>
                          {board.viewCount != null && (
                            <span className={styles.metaItem}>
                              <EyeIcon /> {board.viewCount}
                            </span>
                          )}
                          {board.commentCount != null && (
                            <span className={styles.metaItem}>
                              <ChatBubbleIcon /> {board.commentCount}
                            </span>
                          )}
                        </span>
                      </span>
                    </Link>
                  </li>
                ))
              : resultPage.content.map((place) => (
                  <li key={place.placeId}>
                    <Link to={`/places/${place.placeId}`} className={styles.item}>
                      {resolveImage(place.imageUrl) ? (
                        <img className={styles.itemThumb} src={resolveImage(place.imageUrl)} alt="" />
                      ) : (
                        <span className={styles.itemThumb}>
                          <MapPinIcon />
                        </span>
                      )}
                      <span className={styles.itemBody}>
                        <span className={styles.title}>{place.placeName}</span>
                        <span className={styles.meta}>{place.address}</span>
                      </span>
                    </Link>
                  </li>
                ))}
          </ul>
          {resultPage.content.length === 0 && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}>{tab === 'boards' ? <PencilIcon /> : <MapPinIcon />}</span>
              <p>찜한 항목이 없습니다.</p>
            </div>
          )}
          <Pagination page={resultPage.number} totalPages={resultPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
