import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Layout from '../components/Layout/Layout';
import PrivateRoute from './PrivateRoute';
import NotFoundPage from '../features/error/NotFoundPage';
import RouteErrorPage from '../features/error/RouteErrorPage';
import LoginPage from '../features/auth/LoginPage';
import SignupPage from '../features/auth/SignupPage';
import OAuth2RedirectPage from '../features/auth/OAuth2RedirectPage';
import CompleteProfilePage from '../features/auth/CompleteProfilePage';
import ForgotPasswordPage from '../features/auth/ForgotPasswordPage';
import ResetPasswordPage from '../features/auth/ResetPasswordPage';
import BoardDetailPage from '../features/board/BoardDetailPage';
import PlaceDetailPage from '../features/place/PlaceDetailPage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        errorElement: <RouteErrorPage />,
        children: [
          { index: true, element: <div>Tails</div> },
          { path: 'login', element: <LoginPage /> },
          { path: 'signup', element: <SignupPage /> },
          { path: 'oauth2/redirect', element: <OAuth2RedirectPage /> },
          { path: 'forgot-password', element: <ForgotPasswordPage /> },
          { path: 'reset-password', element: <ResetPasswordPage /> },
          { path: 'boards/:boardId', element: <BoardDetailPage /> },
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
