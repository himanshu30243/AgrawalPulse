import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { FamiliesByChapterDatum } from '@/types/domain';
import { chartColors } from '@/theme';

interface Props {
  data: FamiliesByChapterDatum[];
}

export function FamiliesByChapterChart({ data }: Props) {
  const { t } = useTranslation();

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 8 }}>
        <CartesianGrid stroke="#e1e0d9" vertical={false} />
        <XAxis
          dataKey="chapter"
          tick={{ fill: '#898781', fontSize: 12 }}
          axisLine={{ stroke: '#c3c2b7' }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: '#898781', fontSize: 12 }}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <Tooltip
          formatter={(value: number) => [value, t('dashboard.families')]}
          contentStyle={{ borderRadius: 8, borderColor: '#e1e0d9' }}
        />
        <Bar dataKey="families" fill={chartColors[0]} radius={[4, 4, 0, 0]} maxBarSize={48} />
      </BarChart>
    </ResponsiveContainer>
  );
}
