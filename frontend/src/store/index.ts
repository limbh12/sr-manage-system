import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import srReducer from './srSlice';
import themeReducer from './themeSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    sr: srReducer,
    theme: themeReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
