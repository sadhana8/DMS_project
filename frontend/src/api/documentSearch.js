import client from './client'

/**
 * Advanced document search. Mirrors backend `DocumentSearchController`
 * at GET /document-search/advanced. All filters are optional.
 *
 * params:
 *   name, tag, department, ownerId, ownerEmail,
 *   dateFrom (yyyy-MM-dd or ISO), dateTo (yyyy-MM-dd or ISO),
 *   page, size
 */
export const documentSearchApi = {
  advanced: (params) =>
    client.get('/document-search/advanced', { params }).then(r => r.data),
}
