import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Box from '@mui/material/Box';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';
import { useAuth } from '@/auth/useAuth';
import { iconFor } from './menuIcons';
import type { MenuItem } from '@/types/domain';

export const DRAWER_WIDTH = 240;

interface NavDrawerProps {
  variant: 'permanent' | 'temporary';
  open: boolean;
  onClose: () => void;
}

export function NavDrawer({ variant, open, onClose }: NavDrawerProps) {
  const { t } = useTranslation();
  const { menus, isProfileLoaded } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // Menus arrive flat from GET /me; render top-level entries in the order the server set. Child
  // menus (parentMenuKey !== null) are intentionally skipped here - nothing seeds any yet, and
  // rendering them as if they were top-level would be wrong.
  const topLevel = useMemo(
    () =>
      menus
        .filter((menu) => menu.active && !menu.parentMenuKey && menu.menuPath)
        .slice()
        .sort((a, b) => a.displayOrder - b.displayOrder),
    [menus],
  );

  const label = (menu: MenuItem) => {
    // Prefer a translated label keyed by the stable menuKey; fall back to the server's English
    // menu_name so a menu added at runtime still renders a sensible label with no frontend change.
    const key = `nav.${menu.menuKey}`;
    const translated = t(key);
    return translated === key ? menu.menuName : translated;
  };

  const content = (
    <Box role="navigation" aria-label="Main navigation">
      <Toolbar />
      {!isProfileLoaded ? (
        <Stack spacing={1} sx={{ p: 2 }} aria-label="Loading navigation">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} variant="rounded" height={36} />
          ))}
        </Stack>
      ) : (
        <List>
          {topLevel.map((menu) => {
            const Icon = iconFor(menu.icon);
            const path = menu.menuPath as string;
            const selected = location.pathname.startsWith(path);
            return (
              <ListItemButton
                key={menu.menuKey}
                selected={selected}
                onClick={() => {
                  navigate(path);
                  if (variant === 'temporary') onClose();
                }}
              >
                <ListItemIcon>
                  <Icon color={selected ? 'primary' : 'inherit'} />
                </ListItemIcon>
                <ListItemText primary={label(menu)} />
              </ListItemButton>
            );
          })}
        </List>
      )}
    </Box>
  );

  return (
    <Drawer
      variant={variant}
      open={variant === 'permanent' ? true : open}
      onClose={onClose}
      ModalProps={{ keepMounted: true }}
      sx={{
        width: DRAWER_WIDTH,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
      }}
    >
      {content}
    </Drawer>
  );
}
