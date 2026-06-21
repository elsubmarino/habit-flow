// src/main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { Provider } from 'react-redux'
import { store } from './store'
import App from './App'
import { ToastProvider } from './context/ToastContext'
import { DialogProvider } from './context/DialogContext'
import { queryClient } from './lib/queryClient'
import './index.css' // 전역 스타일

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <Provider store={store}>
                <ToastProvider>
                    <DialogProvider>
                        <App />
                    </DialogProvider>
                </ToastProvider>
            </Provider>
        </QueryClientProvider>
    </React.StrictMode>,
)