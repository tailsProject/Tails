import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Layout from '../components/Layout/Layout';
import PrivateRoute from './PrivateRoute';
import NotFoundPage from '../features/error/NotFoundPage';
import RouteErrorPage from '../features/error/RouteErrorPage';
import TravelListPage from '../features/travel/TravelListPage';
import TravelDetailPage from '../features/travel/TravelDetailPage';
import SharedTravelPage from '../features/travel/SharedTravelPage';
import LoginPage from '../features/auth/LoginPage';
import SignupPage from '../features/auth/SignupPage';
import OAuth2RedirectPage from '../features/auth/OAuth2RedirectPage';
import CompleteProfilePage from '../features/auth/CompleteProfilePage';
import ForgotPasswordPage from '../features/auth/ForgotPasswordPage';
import ResetPasswordPage from '../features/auth/ResetPasswordPage';
import PlaceMapPage from '../features/place/PlaceMapPage';
import PlaceDetailPage from '../features/place/PlaceDetailPage';
import BoardListPage from '../features/board/BoardListPage';
import BoardDetailPage from '../features/board/BoardDetailPage';
import BoardWritePage from '../features/board/BoardWritePage';

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
          { path: 'login', element: <LoginPage /> },
          { path: 'signup', element: <SignupPage /> },
          { path: 'oauth2/redirect', element: <OAuth2RedirectPage /> },
          { path: 'forgot-password', element: <ForgotPasswordPage /> },
          { path: 'reset-password', element: <ResetPasswordPage /> },
          { path: 'places', element: <PlaceMapPage /> },
          { path: 'places/:placeId', element: <PlaceDetailPage /> },
          { path: 'boards', element: <BoardListPage /> },
          { path: 'boards/:boardId', element: <BoardDetailPage /> },
          {
            element: <PrivateRoute />,
            children: [
              { path: 'complete-profile', element: <CompleteProfilePage /> },
              { path: 'boards/new', element: <BoardWritePage /> },
              { path: 'boards/:boardId/edit', element: <BoardWritePage /> },
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
