import { registerPlugin } from '@capacitor/core';

import type { PlugPagPlugin } from './definitions';

const PlugPag = registerPlugin<PlugPagPlugin>('PlugPag', {});

export * from './definitions';
export { PlugPag };
