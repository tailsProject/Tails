import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import { ToastProvider } from './components/Toast/ToastContext';
import { ConfirmProvider } from './components/Modal/ConfirmContext';
import Router from './routes/Router';

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <ToastProvider>
          <ConfirmProvider>
            <Router />
          </ConfirmProvider>
        </ToastProvider>
      </NotificationProvider>
    </AuthProvider>
  );
}
