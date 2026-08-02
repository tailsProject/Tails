import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Layout from '../components/Layout/Layout';
import PrivateRoute from './PrivateRoute';
import NotFoundPage from '../features/error/NotFoundPage';
import RouteErrorPage from '../features/error/RouteErrorPage';
import TravelListPage from '../features/travel/TravelListPage';
import TravelDetailPage from '../features/travel/TravelDetailPage';
import SharedTravelPage from '../features/travel/SharedTravelPage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        errorElement: <RouteErrorPage />,
        children: [
          { index: true, element: <div>Tails</div> },
          { path: 'travels/shared/:shareToken', element: <SharedTravelPage /> },
          {
            element: <PrivateRoute />,
            children: [
              { path: 'travels', element: <TravelListPage /> },
              { path: 'travels/:travelId', element: <TravelDetailPage /> },
            ],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
