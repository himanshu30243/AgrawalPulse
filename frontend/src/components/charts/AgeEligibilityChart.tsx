import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { AgeEligibilityDatum } from '@/types/domain';
import { chartColors } from '@/theme';

interface Props {
  data: AgeEligibilityDatum[];
}

export function AgeEligibilityChart({ data }: Props) {
  const { t } = useTranslation();

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 8 }} barGap={2}>
        <CartesianGrid stroke="#e1e0d9" vertical={false} />
        <XAxis
          dataKey="ageBand"
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
        <Tooltip contentStyle={{ borderRadius: 8, borderColor: '#e1e0d9' }} />
        <Legend
          formatter={(value: string) =>
            value === 'boys' ? t('dashboard.boys') : t('dashboard.girls')
          }
        />
        <Bar dataKey="boys" fill={chartColors[0]} radius={[4, 4, 0, 0]} maxBarSize={28} />
        <Bar dataKey="girls" fill={chartColors[1]} radius={[4, 4, 0, 0]} maxBarSize={28} />
      </BarChart>
    </ResponsiveContainer>
  );
}
