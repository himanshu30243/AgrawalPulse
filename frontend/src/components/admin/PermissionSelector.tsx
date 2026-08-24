import { useState, useMemo } from 'react';
import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Card from '@mui/material/Card';

export interface Permission {
  // The permission's stable identity. A string because the backend keys permissions by their
  // code (VIEW_FAMILY, ...), which is also what role_permissions is written in terms of - there
  // is no numeric id to round-trip.
  id: string;
  code: string;
  name: string;
  category: string;
  description?: string;
  isActive: boolean;
}

interface PermissionSelectorProps {
  permissions: Permission[];
  selectedIds: string[];
  onSelectionChange: (selectedIds: string[]) => void;
  groupByCategory?: boolean;
}

export function PermissionSelector({
  permissions,
  selectedIds,
  onSelectionChange,
  groupByCategory = true,
}: PermissionSelectorProps) {
  const [searchTerm, setSearchTerm] = useState('');

  const grouped = useMemo(() => {
    const filtered = permissions.filter(
      (p) =>
        p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.description?.toLowerCase().includes(searchTerm.toLowerCase())
    );

    if (!groupByCategory) return { ungrouped: filtered };

    return filtered.reduce(
      (acc, perm) => {
        const cat = perm.category || 'Other';
        if (!acc[cat]) acc[cat] = [];
        acc[cat].push(perm);
        return acc;
      },
      {} as Record<string, Permission[]>
    );
  }, [permissions, searchTerm, groupByCategory]);

  const handlePermissionChange = (permissionId: string, checked: boolean) => {
    const newSelection = checked
      ? [...selectedIds, permissionId]
      : selectedIds.filter((id) => id !== permissionId);
    onSelectionChange(newSelection);
  };

  const handleSelectAll = (checked: boolean) => {
    const allIds = permissions.map((p) => p.id);
    onSelectionChange(checked ? allIds : []);
  };

  const allPermissionsSelected =
    selectedIds.length > 0 && selectedIds.length === permissions.length;
  const somePermissionsSelected =
    selectedIds.length > 0 && selectedIds.length < permissions.length;

  return (
    <Stack spacing={2}>
      {/* Search */}
      <TextField
        fullWidth
        size="small"
        placeholder="Search permissions..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        sx={{
          '& .MuiOutlinedInput-root': {
            backgroundColor: '#FFFFFF',
          },
        }}
      />

      {/* Select All */}
      <FormControlLabel
        control={
          <Checkbox
            checked={allPermissionsSelected}
            indeterminate={somePermissionsSelected}
            onChange={(e) => handleSelectAll(e.target.checked)}
          />
        }
        label={
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            Select All Permissions
          </Typography>
        }
      />

      {/* Permissions by Category */}
      <Stack spacing={2}>
        {Object.entries(grouped).map(([category, perms]) => (
          <Card key={category} variant="outlined" sx={{ p: 2 }}>
            <Typography
              variant="subtitle2"
              sx={{
                fontWeight: 600,
                color: '#7C1D1D',
                mb: 1.5,
                textTransform: 'uppercase',
                fontSize: '12px',
                letterSpacing: '0.05em',
              }}
            >
              {category}
            </Typography>

            <Stack spacing={1}>
              {perms.map((permission) => (
                <FormControlLabel
                  key={permission.id}
                  control={
                    <Checkbox
                      checked={selectedIds.includes(permission.id)}
                      onChange={(e) =>
                        handlePermissionChange(permission.id, e.target.checked)
                      }
                      disabled={!permission.isActive}
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {permission.name}
                      </Typography>
                      <Typography
                        variant="caption"
                        sx={{
                          color: '#65676B',
                          display: 'block',
                          mt: 0.25,
                        }}
                      >
                        <code>{permission.code}</code>
                        {permission.description && ` — ${permission.description}`}
                      </Typography>
                    </Box>
                  }
                  sx={{
                    mb: 1,
                    opacity: permission.isActive ? 1 : 0.5,
                  }}
                />
              ))}
            </Stack>
          </Card>
        ))}
      </Stack>

      {/* Selection Summary */}
      {selectedIds.length > 0 && (
        <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F0F2F5', borderRadius: '8px' }}>
          <Typography variant="caption" sx={{ color: '#65676B' }}>
            <strong>{selectedIds.length}</strong> permission{selectedIds.length !== 1 ? 's' : ''}{' '}
            selected
          </Typography>
        </Box>
      )}
    </Stack>
  );
}
