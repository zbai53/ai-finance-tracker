import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function DashboardPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className='p-8'>
      <div className='flex justify-between items-center mb-6'>
        <h1 className='text-2xl font-bold'>Dashboard</h1>
        <div className='flex gap-4'>
          <button
            onClick={() => navigate('/transactions')}
            className='bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700'
          >
            Transaction
          </button>
          <button
            onClick={logout}
            className='bg-gray-200 text-gray-700 px-4 py-2 rounded-md hover:bg-gray-300'
          >
            Logout
          </button>
        </div>
      </div>
      <p className="text-gray-500">Charts coming in Day 12.</p>
    </div>
  )
}
