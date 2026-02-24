import { registerPlugin } from '@capacitor/core';

import type { PlugPagPluginPlugin } from './definitions';

const PlugPagPlugin = registerPlugin<PlugPagPluginPlugin>('PlugPagPlugin', {
  web: () => import('./web').then((m) => new m.PlugPagPluginWeb()),
});

export * from './definitions';
export { PlugPagPlugin };
