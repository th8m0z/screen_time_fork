//
//  ScreenTimeMethod.swift
//  Pods
//
//  Created by Chandra Abdul Fattah on 29/04/25.
//

import FamilyControls

class ScreenTimeMethod {
    static func requestPermission(
        type: ScreenTimePermissionType = ScreenTimePermissionType.appUsage
    ) async -> Bool {
        switch type {
            case ScreenTimePermissionType.appUsage,
                 ScreenTimePermissionType.accessibilitySettings,
                 ScreenTimePermissionType.drawOverlay:
                do {
                    try await AuthorizationCenter.shared.requestAuthorization(for: FamilyControlsMember.individual)
                    print("Request Permission Launched")
                    return true
                } catch {
                    print("Request Permission Failed: \(error.localizedDescription)")
                    return false
                }
            case ScreenTimePermissionType.notification:
                do {
                    try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
                    return true
                } catch {
                    return false
                }
        }
    }
    
    static func permissionStatus(type: ScreenTimePermissionType = ScreenTimePermissionType.appUsage) async -> ScreenTimePermissionStatus {
        switch type {
            case ScreenTimePermissionType.appUsage,
                 ScreenTimePermissionType.accessibilitySettings,
                 ScreenTimePermissionType.drawOverlay:
                let status = AuthorizationCenter.shared.authorizationStatus
                let statusEnum = status == .approved ? ScreenTimePermissionStatus.approved :
                    status == .denied ? ScreenTimePermissionStatus.denied : ScreenTimePermissionStatus.notDetermined
                return statusEnum
            case ScreenTimePermissionType.notification:
                return await withCheckedContinuation { continuation in
                    UNUserNotificationCenter.current().getNotificationSettings { settings in
                        let status: ScreenTimePermissionStatus
                        switch settings.authorizationStatus {
                            case .authorized:
                                status = .approved
                            case .denied:
                                status = .denied
                            default:
                                status = .notDetermined
                        }
                        continuation.resume(returning: status)
                    }
                }
        }
    }
}
