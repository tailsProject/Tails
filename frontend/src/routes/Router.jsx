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
import BoardDetailPage from '../features/board/BoardDetailPage';
import PlaceMapPage from '../features/place/PlaceMapPage';
import PlaceDetailPage from '../features/place/PlaceDetailPage';
import MainPage from '../features/main/MainPage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        errorElement: <RouteErrorPage />,
        children: [
          { index: true, element: <MainPage /> },
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
          { path: 'boards/:boardId', element: <BoardDetailPage /> },
          { path: 'places', element: <PlaceMapPage /> },
          { path: 'places/:placeId', element: <PlaceDetailPage /> },
          {
            element: <PrivateRoute />,
            children: [{ path: 'complete-profile', element: <CompleteProfilePage /> }],
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
