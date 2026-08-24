import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import { AppBarTop } from './AppBarTop';
import { NavDrawer, DRAWER_WIDTH } from './NavDrawer';

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBarTop onMenuClick={() => setMobileOpen(true)} />

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Box sx={{ display: { xs: 'block', md: 'none' } }}>
          <NavDrawer variant="temporary" open={mobileOpen} onClose={() => setMobileOpen(false)} />
        </Box>
        <Box sx={{ display: { xs: 'none', md: 'block' } }}>
          <NavDrawer variant="permanent" open onClose={() => undefined} />
        </Box>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, sm: 3 },
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          minHeight: '100vh',
          bgcolor: 'background.default',
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
