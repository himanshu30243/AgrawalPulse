import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';

interface StatTileProps {
  label: string;
  value: string;
}

export function StatTile({ label, value }: StatTileProps) {
  return (
    <Paper variant="outlined" sx={{ p: 2, height: '100%' }}>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        {label}
      </Typography>
      <Typography variant="h5" fontWeight={600} sx={{ fontVariantNumeric: 'proportional-nums' }}>
        {value}
      </Typography>
    </Paper>
  );
}
