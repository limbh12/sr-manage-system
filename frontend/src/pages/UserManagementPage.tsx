import UserList from '../components/user/UserList';

/**
 * 사용자 관리 페이지
 */
function UserManagementPage() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">사용자 관리</h1>
      </div>
      <UserList />
    </div>
  );
}

export default UserManagementPage;
