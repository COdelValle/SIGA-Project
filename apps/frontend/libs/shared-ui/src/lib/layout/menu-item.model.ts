export type MenuIconName =
  | 'home'
  | 'clock'
  | 'notes'
  | 'attendance'
  | 'courses'
  | 'requests'
  | 'users'
  | 'roles'
  | 'subjects'
  | 'curriculum'
  | 'progress'
  | 'register-notes'
  | 'register-attendance';

export interface MenuItem {
  label: string;
  route: string;
  icon: MenuIconName;
}
