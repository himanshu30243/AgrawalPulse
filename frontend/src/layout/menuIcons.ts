import DashboardIcon from '@mui/icons-material/Dashboard';
import FamilyIcon from '@mui/icons-material/FamilyRestroom';
import MembershipIcon from '@mui/icons-material/CardMembership';
import MatrimonyConsentIcon from '@mui/icons-material/HowToReg';
import MatrimonyDirectoryIcon from '@mui/icons-material/Favorite';
import EventsIcon from '@mui/icons-material/Event';
import ReportsIcon from '@mui/icons-material/Assessment';
import PeopleIcon from '@mui/icons-material/People';
import BranchIcon from '@mui/icons-material/AccountTree';
import AdminIcon from '@mui/icons-material/AdminPanelSettings';
import SettingsIcon from '@mui/icons-material/Settings';
import CircleIcon from '@mui/icons-material/RadioButtonUnchecked';
import type { SvgIconComponent } from '@mui/icons-material';

// Maps the `icon` string a menu row carries to a real component. Icons can't come from the
// database as components, so this is the one unavoidable piece of client-side knowledge about
// menus - but an unknown name degrades to a neutral bullet rather than crashing, so adding a menu
// server-side still works with no frontend release. Ship a matching entry here to give it a
// proper glyph.
const ICONS: Record<string, SvgIconComponent> = {
  Dashboard: DashboardIcon,
  FamilyRestroom: FamilyIcon,
  CardMembership: MembershipIcon,
  HowToReg: MatrimonyConsentIcon,
  Favorite: MatrimonyDirectoryIcon,
  Event: EventsIcon,
  Assessment: ReportsIcon,
  People: PeopleIcon,
  AccountTree: BranchIcon,
  AdminPanelSettings: AdminIcon,
  Settings: SettingsIcon,
};

export function iconFor(name: string | null | undefined): SvgIconComponent {
  if (!name) return CircleIcon;
  return ICONS[name] ?? CircleIcon;
}
