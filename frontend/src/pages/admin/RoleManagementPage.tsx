import { useCallback, useMemo, useState } from 'react';
import Add from '@mui/icons-material/Add';
import Edit from '@mui/icons-material/Edit';
import LockIcon from '@mui/icons-material/Lock';
import MenuIcon from '@mui/icons-material/ViewList';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import IconButton from '@mui/material/IconButton';
import Switch from '@mui/material/Switch';
import Alert from '@mui/material/Alert';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { PermissionSelector } from '@/components/admin/PermissionSelector';
import { menusApi, permissionsApi, rolesApi } from '@/api/rbacApi';
import type { RoleDetail } from '@/api/rbacApi';

type DialogMode = 'create' | 'edit' | 'permissions' | 'menus' | null;

export default function RoleManagementPage() {
  const { data: roles, isLoading, error, reload } = useAsync(() => rolesApi.list(), []);
  const { data: permissions } = useAsync(() => permissionsApi.list(), []);
  const { data: menus } = useAsync(() => menusApi.list(), []);

  const [mode, setMode] = useState<DialogMode>(null);
  const [active, setActive] = useState<RoleDetail | null>(null);
  const [form, setForm] = useState({ roleCode: '', roleName: '', description: '' });
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);
  const [selectedMenus, setSelectedMenus] = useState<string[]>([]);
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return roles ?? [];
    return (roles ?? []).filter(
      (role) =>
        role.roleName.toLowerCase().includes(query) ||
        role.roleCode.toLowerCase().includes(query) ||
        (role.description ?? '').toLowerCase().includes(query),
    );
  }, [roles, search]);

  const close = () => {
    setMode(null);
    setActive(null);
    setFormError(null);
  };

  const openCreate = () => {
    setForm({ roleCode: '', roleName: '', description: '' });
    setActive(null);
    setFormError(null);
    setMode('create');
  };

  const openEdit = (role: RoleDetail) => {
    setForm({ roleCode: role.roleCode, roleName: role.roleName, description: role.description ?? '' });
    setActive(role);
    setFormError(null);
    setMode('edit');
  };

  const openPermissions = (role: RoleDetail) => {
    setActive(role);
    setSelectedPermissions(role.permissionCodes);
    setFormError(null);
    setMode('permissions');
  };

  const openMenus = (role: RoleDetail) => {
    setActive(role);
    setSelectedMenus(role.menuKeys);
    setFormError(null);
    setMode('menus');
  };

  // Every mutation funnels through here so a failed call always surfaces the server's message
  // instead of silently closing the dialog as if it had worked.
  const run = useCallback(
    async (action: () => Promise<unknown>) => {
      setBusy(true);
      setFormError(null);
      try {
        await action();
        reload();
        close();
      } catch (err) {
        setFormError(err instanceof Error ? err.message : 'The change could not be saved.');
      } finally {
        setBusy(false);
      }
    },
    [reload],
  );

  const saveRole = () => {
    if (!form.roleName.trim()) {
      setFormError('Role name is required.');
      return;
    }
    if (mode === 'create') {
      if (!form.roleCode.trim()) {
        setFormError('Role code is required.');
        return;
      }
      void run(() =>
        rolesApi.create({
          roleCode: form.roleCode.trim().toUpperCase(),
          roleName: form.roleName.trim(),
          description: form.description.trim(),
        }),
      );
    } else if (active) {
      void run(() =>
        rolesApi.update(active.roleId, {
          roleName: form.roleName.trim(),
          description: form.description.trim(),
        }),
      );
    }
  };

  const permissionOptions = useMemo(
    () =>
      (permissions ?? []).map((permission) => ({
        id: permission.permissionCode,
        code: permission.permissionCode,
        name: permission.permissionName,
        // Group by the code's prefix (VIEW_FAMILY -> FAMILY) so related grants sit together
        // without needing a category column on the table.
        category: permission.permissionCode.split('_').slice(1).join(' ') || 'General',
        description: permission.description ?? undefined,
        isActive: true,
      })),
    [permissions],
  );

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2} sx={{ mb: 3 }}>
        <Stack spacing={0.5}>
          <Typography variant="h5" fontWeight={600}>
            Role Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Roles, and the permissions and menus each one grants
          </Typography>
        </Stack>
        <Button variant="contained" startIcon={<Add />} onClick={openCreate}>
          Create Role
        </Button>
      </Stack>

      <TextField
        fullWidth
        size="small"
        placeholder="Search roles by name, code or description"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 3, maxWidth: 420 }}
      />

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {roles && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Role</TableCell>
                <TableCell>Description</TableCell>
                <TableCell align="center">Permissions</TableCell>
                <TableCell align="center">Menus</TableCell>
                <TableCell align="center">Active</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((role) => (
                <TableRow key={role.roleId} hover>
                  <TableCell>
                    <Stack spacing={0.25}>
                      <Typography variant="body2" fontWeight={600}>
                        {role.roleName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {role.roleCode}
                      </Typography>
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {role.description || '—'}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Chip label={role.permissionCodes.length} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell align="center">
                    <Chip label={role.menuKeys.length} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell align="center">
                    <Switch
                      checked={role.active}
                      onChange={(e) => void run(() => rolesApi.setActive(role.roleId, e.target.checked))}
                      inputProps={{ 'aria-label': `Activate ${role.roleName}` }}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Assign permissions">
                      <IconButton size="small" onClick={() => openPermissions(role)} aria-label={`Permissions for ${role.roleName}`}>
                        <LockIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Assign menus">
                      <IconButton size="small" onClick={() => openMenus(role)} aria-label={`Menus for ${role.roleName}`}>
                        <MenuIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Edit role">
                      <IconButton size="small" onClick={() => openEdit(role)} aria-label={`Edit ${role.roleName}`}>
                        <Edit fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {roles && filtered.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No roles match your search.</Typography>
        </Paper>
      )}

      {/* Create / edit */}
      <Dialog open={mode === 'create' || mode === 'edit'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>{mode === 'create' ? 'Create Role' : `Edit ${active?.roleName ?? ''}`}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            <TextField
              label="Role Code"
              value={form.roleCode}
              onChange={(e) => setForm((prev) => ({ ...prev, roleCode: e.target.value }))}
              // Immutable after creation: the code is what @PreAuthorize and the JWT reference,
              // so renaming it would silently strip access from everyone holding the role.
              disabled={mode === 'edit'}
              helperText={mode === 'edit' ? 'Role codes cannot be changed once in use' : 'e.g. TREASURER'}
              fullWidth
            />
            <TextField
              label="Role Name"
              value={form.roleName}
              onChange={(e) => setForm((prev) => ({ ...prev, roleName: e.target.value }))}
              fullWidth
            />
            <TextField
              label="Description"
              value={form.description}
              onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
              multiline
              rows={3}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={saveRole} disabled={busy}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Permissions */}
      <Dialog open={mode === 'permissions'} onClose={close} maxWidth="md" fullWidth>
        <DialogTitle>Permissions — {active?.roleName}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <PermissionSelector
            permissions={permissionOptions}
            selectedIds={selectedPermissions}
            onSelectionChange={setSelectedPermissions}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button
            variant="contained"
            disabled={busy}
            onClick={() => active && void run(() => rolesApi.setPermissions(active.roleId, selectedPermissions))}
          >
            Save Permissions
          </Button>
        </DialogActions>
      </Dialog>

      {/* Menus */}
      <Dialog open={mode === 'menus'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Menus — {active?.roleName}</DialogTitle>
        <DialogContent>
          {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
            Controls which entries appear in this role&apos;s dashboard navigation.
          </Typography>
          <FormGroup>
            {(menus ?? []).map((menu) => (
              <FormControlLabel
                key={menu.menuKey}
                control={
                  <Checkbox
                    checked={selectedMenus.includes(menu.menuKey)}
                    onChange={(e) =>
                      setSelectedMenus((prev) =>
                        e.target.checked ? [...prev, menu.menuKey] : prev.filter((key) => key !== menu.menuKey),
                      )
                    }
                  />
                }
                label={
                  <Stack spacing={0}>
                    <Typography variant="body2">{menu.menuName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {menu.menuPath ?? menu.menuKey}
                    </Typography>
                  </Stack>
                }
              />
            ))}
          </FormGroup>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button
            variant="contained"
            disabled={busy}
            onClick={() => active && void run(() => rolesApi.setMenus(active.roleId, selectedMenus))}
          >
            Save Menus
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
