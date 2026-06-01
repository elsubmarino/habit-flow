// store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import habitReducer from './habitSlice';
import notificationsReducer from './notificationsSlice';

export const store = configureStore({
    reducer: { habits: habitReducer, notifications: notificationsReducer },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;