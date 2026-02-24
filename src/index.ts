import { registerPlugin } from '@capacitor/core';

import type { PlugPagPluginPlugin } from './definitions';

const PlugPagPlugin = registerPlugin<PlugPagPluginPlugin>('PlugPagPlugin', {});

export * from './definitions';
export { PlugPagPlugin };
