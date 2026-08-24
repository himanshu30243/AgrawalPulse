import { useTranslation } from 'react-i18next';
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { MembershipStatusDatum, MembershipStatus } from '@/types/domain';
import { statusColors } from '@/theme';

interface Props {
  data: MembershipStatusDatum[];
}

const STATUS_COLOR: Record<MembershipStatus, string> = {
  ACTIVE: statusColors.good,
  PENDING: statusColors.warning,
  INACTIVE: statusColors.critical,
};

const STATUS_LABEL_KEY: Record<MembershipStatus, string> = {
  ACTIVE: 'membership.statusActive',
  PENDING: 'membership.statusPending',
  INACTIVE: 'membership.statusInactive',
};

export function MembershipStatusChart({ data }: Props) {
  const { t } = useTranslation();
  const chartData = data.map((d) => ({ ...d, label: t(STATUS_LABEL_KEY[d.status]) }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie
          data={chartData}
          dataKey="count"
          nameKey="label"
          innerRadius={60}
          outerRadius={90}
          paddingAngle={2}
          stroke="#fcfcfb"
          strokeWidth={2}
        >
          {chartData.map((entry) => (
            <Cell key={entry.status} fill={STATUS_COLOR[entry.status]} />
          ))}
        </Pie>
        <Tooltip contentStyle={{ borderRadius: 8, borderColor: '#e1e0d9' }} />
        <Legend
          verticalAlign="bottom"
          height={36}
          formatter={(value: string) => (
            <span style={{ color: '#52514e', fontSize: 13 }}>{value}</span>
          )}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}
