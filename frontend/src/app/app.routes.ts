import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'feedback' },
  {
    path: 'feedback',
    title: 'Course feedback',
    loadComponent: () => import('./feedback/feedback-page').then((m) => m.FeedbackPage),
  },
  { path: '**', redirectTo: 'feedback' },
];
