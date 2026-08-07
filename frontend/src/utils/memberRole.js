// ADMIN/MANAGER는 관리 기능(모더레이션, 관리자 페이지 접근)이 동일하게 필요함
export function isStaffRole(role) {
  return role === 'ADMIN' || role === 'MANAGER';
}
