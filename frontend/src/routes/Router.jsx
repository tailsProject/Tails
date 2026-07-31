import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import PrivateRoute from './PrivateRoute';

const router = createBrowserRouter([
  { path: '/', element: <div>Tails</div> },
  { element: <PrivateRoute />, children: [] },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
