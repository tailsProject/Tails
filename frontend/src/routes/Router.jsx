import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Layout from '../components/Layout/Layout';
import PrivateRoute from './PrivateRoute';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <div>Tails</div> },
      { element: <PrivateRoute />, children: [] },
    ],
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
