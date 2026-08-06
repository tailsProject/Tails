// 관리자 회원 검색, 권한 변경, 강제 추방 페이지
import { useEffect, useState } from 'react';
import { getMembers, changeMemberRole, expelMember } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import { useAuth } from '../../hooks/useAuth';
import Pagination from '../../components/Pagination/Pagination';
import StateMessage from '../../components/StateMessage/StateMessage';
import { MagnifyingGlassIcon, WarningIcon } from '../../components/Icon/Icon';
import styles from './AdminMembersPage.module.scss';

export default function AdminMembersPage() {
  const { showToast } = useToast();
  const confirm = useConfirm();
  const { member: me } = useAuth();
  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [page, setPage] = useState(0);
  const [memberPage, setMemberPage] = useState(null);
  const [accessDenied, setAccessDenied] = useState(false);

  async function load() {
    try {
      const res = await getMembers({ keyword, page });
      setMemberPage(res.data.data);
    } catch (error) {
      if (error.response?.status === 403) {
        setAccessDenied(true);
      }
    }
  }

  useEffect(() => {
    load();
  }, [keyword, page]);

  function handleSearchSubmit(e) {
    e.preventDefault();
    setPage(0);
    setKeyword(keywordInput);
  }

  async function handleRoleChange(target, nextRole) {
    if (nextRole === target.role) return;
    const ok = await confirm(
      `${target.nickname}(${target.email})님의 권한을 ${nextRole === 'ADMIN' ? '관리자' : '회원'}로 변경하시겠습니까?`,
    );
    if (!ok) return;
    try {
      await changeMemberRole(target.memberId, nextRole);
      showToast('권한이 변경되었습니다.', 'success');
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '변경에 실패했습니다.', 'error');
    }
  }

  async function handleExpel(target) {
    const ok = await confirm(
      `${target.nickname}(${target.email})님을 추방하시겠습니까?\n작성한 글/댓글/리뷰는 "탈퇴한 회원"으로 남고, 되돌릴 수 없습니다.`,
    );
    if (!ok) return;
    try {
      await expelMember(target.memberId);
      showToast('추방되었습니다.', 'success');
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '추방에 실패했습니다.', 'error');
    }
  }

  if (accessDenied) {
    return (
      <StateMessage
        icon={WarningIcon}
        title="접근 권한이 없어요"
        description="관리자만 볼 수 있는 페이지예요."
        actionTo="/mypage"
        actionLabel="마이페이지로"
      />
    );
  }

  return (
    <div>
      <div className={styles.header}>
        <h1>회원 권한 변경</h1>
        {memberPage && <span className={styles.count}>전체 {memberPage.totalElements.toLocaleString()}명</span>}
      </div>

      <form onSubmit={handleSearchSubmit} className={styles.searchForm}>
        <span className={styles.searchIcon}>
          <MagnifyingGlassIcon />
        </span>
        <input
          type="text"
          placeholder="이메일 또는 닉네임 검색"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
        />
        <button type="submit">검색</button>
      </form>

      {memberPage && (
        <>
          <div className={styles.card}>
            <div className={styles.tableScroll}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>회원</th>
                    <th>이메일</th>
                    <th>가입일</th>
                    <th>권한</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {memberPage.content.map((m) => (
                    <tr key={m.memberId}>
                      <td>
                        <div className={styles.memberCell}>
                          <span className={styles.avatar}>{m.nickname[0]}</span>
                          {m.nickname}
                        </div>
                      </td>
                      <td>{m.email}</td>
                      <td>{new Date(m.createdAt).toLocaleDateString()}</td>
                      <td>
                        {/* 자기 자신의 권한 변경과 추방은 버튼 단계에서부터 막음 */}
                        <select
                          className={m.role === 'ADMIN' ? styles.roleSelectAdmin : styles.roleSelect}
                          value={m.role}
                          disabled={m.memberId === me?.memberId}
                          onChange={(e) => handleRoleChange(m, e.target.value)}
                        >
                          <option value="ADMIN">관리자</option>
                          <option value="USER">회원</option>
                        </select>
                      </td>
                      <td>
                        <button
                          type="button"
                          className={styles.expelButton}
                          disabled={m.memberId === me?.memberId}
                          onClick={() => handleExpel(m)}
                        >
                          추방
                        </button>
                      </td>
                    </tr>
                  ))}
                  {memberPage.content.length === 0 && (
                    <tr>
                      <td colSpan={5} className={styles.emptyRow}>
                        검색 결과가 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
          <Pagination page={memberPage.number} totalPages={memberPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
