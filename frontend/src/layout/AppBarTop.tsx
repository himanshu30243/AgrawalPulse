import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import MenuIcon from '@mui/icons-material/Menu';
import Box from '@mui/material/Box';
import Avatar from '@mui/material/Avatar';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '@/auth/useAuth';
import { branchesApi } from '@/api/branchesApi';
import { LanguageSwitcher } from './LanguageSwitcher';
import { DRAWER_WIDTH } from './NavDrawer';

interface AppBarTopProps {
  onMenuClick: () => void;
}

function initialsOf(email: string): string {
  const localPart = email.split('@')[0] ?? email;
  return localPart.slice(0, 2).toUpperCase();
}

export function AppBarTop({ onMenuClick }: AppBarTopProps) {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  // The JWT never actually carries a chapter_name claim (only chapter_id - see auth/types.ts),
  // so the branch name shown here is resolved live rather than trusted from the token.
  const [branchLabel, setBranchLabel] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    branchesApi
      .list()
      .then((branches) => {
        const branch = branches.find((b) => b.id === user.chapterId);
        setBranchLabel(branch ? `${branch.name} (${branch.city})` : null);
      })
      .catch(() => setBranchLabel(null));
  }, [user]);

  return (
    <AppBar
      position="fixed"
      sx={{
        width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
        ml: { md: `${DRAWER_WIDTH}px` },
      }}
    >
      <Toolbar>
        <IconButton
          color="inherit"
          edge="start"
          onClick={onMenuClick}
          sx={{ mr: 2, display: { md: 'none' } }}
          aria-label="Open navigation"
        >
          <MenuIcon />
        </IconButton>
        <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
          {t('app.name')}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <LanguageSwitcher />
          {user && (
            <>
              <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ p: 0 }}>
                <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>{initialsOf(user.email)}</Avatar>
              </IconButton>
              <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
                <MenuItem disabled sx={{ opacity: '1 !important' }}>
                  <Box>
                    <Typography variant="body2">{user.email}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {branchLabel ?? user.chapterId}
                    </Typography>
                  </Box>
                </MenuItem>
                <MenuItem
                  onClick={() => {
                    setAnchorEl(null);
                    logout();
                  }}
                >
                  <ListItemIcon>
                    <LogoutIcon fontSize="small" />
                  </ListItemIcon>
                  {t('nav.logout')}
                </MenuItem>
              </Menu>
            </>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
