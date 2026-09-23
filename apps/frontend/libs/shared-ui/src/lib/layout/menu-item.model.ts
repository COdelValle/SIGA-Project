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
  | 'progress'
  | 'register-notes'
  | 'register-attendance';

export interface MenuItem {
  label: string;
  route: string;
  icon: MenuIconName;
}
